package pt.isep.bookmarkscc.pteid;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PteidCardPoller {

    public interface Listener {
        void onInserted();
        void onRemoved();
        void onError(String msg, Throwable t);
    }

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pteid-poller");
                t.setDaemon(true);
                return t;
            });

    private final AtomicBoolean lastPresent = new AtomicBoolean(false);
    private ScheduledFuture<?> task;

    public synchronized void start(Listener listener) {
        try {
            if (task != null) {
                task.cancel(true);
                task = null;
            }

            PteidSdk.stop();
            PteidSdk.start();

            boolean present = PteidSdk.isCardPresent();
            lastPresent.set(present);
            if (present) listener.onInserted();

            task = scheduler.scheduleAtFixedRate(() -> {
                try {
                    boolean now = PteidSdk.isCardPresent();
                    boolean prev = lastPresent.getAndSet(now);

                    if (!prev && now) listener.onInserted();
                    if (prev && !now) listener.onRemoved();

                } catch (Throwable t) {
                    listener.onError("Erro no polling do cartão.", t);
                }
            }, 500, 500, TimeUnit.MILLISECONDS);

        } catch (Throwable t) {
            listener.onError("Falha ao iniciar polling do cartão.", t);
        }
    }

    public synchronized void stop() {
        try {
            if (task != null) task.cancel(true);
        } catch (Throwable ignored) {
        } finally {
            task = null;
            PteidSdk.stop();
        }
    }
}

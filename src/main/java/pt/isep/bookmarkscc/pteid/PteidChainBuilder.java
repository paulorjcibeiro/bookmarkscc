package pt.isep.bookmarkscc.pteid;

import java.security.cert.X509Certificate;
import java.util.*;

public final class PteidChainBuilder {

    public record Chain(X509Certificate leaf, X509Certificate subCa, X509Certificate root) {}

    private PteidChainBuilder() {}

    public static Chain buildAndValidateAuthChain(X509Certificate authLeaf, List<X509Certificate> pool) throws Exception {
        // Index por Subject para procurar rapidamente
        Map<String, List<X509Certificate>> bySubject = new HashMap<>();
        for (X509Certificate c : pool) {
            bySubject.computeIfAbsent(c.getSubjectX500Principal().getName(), k -> new ArrayList<>()).add(c);
        }

        // 1) Encontrar SubCA cujo Subject == Issuer do leaf
        String leafIssuer = authLeaf.getIssuerX500Principal().getName();
        List<X509Certificate> subCandidates = bySubject.getOrDefault(leafIssuer, List.of());
        if (subCandidates.isEmpty()) {
            throw new IllegalStateException("Nao encontrei SubCA com Subject == Issuer do AUTH.");
        }

        Exception last = null;

        for (X509Certificate subCa : subCandidates) {
            try {
                // Validação mínima (opção A): datas só para leaf (e opcionalmente subca)
                authLeaf.checkValidity();
                subCa.checkValidity();

                // auth -> sub
                authLeaf.verify(subCa.getPublicKey());

                // 2) Encontrar Root cujo Subject == Issuer da SubCA
                String subIssuer = subCa.getIssuerX500Principal().getName();
                List<X509Certificate> rootCandidates = bySubject.getOrDefault(subIssuer, List.of());
                if (rootCandidates.isEmpty()) {
                    // não é necessariamente erro: pode haver intermediários adicionais, mas no teu conjunto
                    // tens roots/CA suficientes. Continuamos para outra SubCA candidata.
                    continue;
                }

                for (X509Certificate root : rootCandidates) {
                    try {
                        // Root: NAO validar datas (pode estar expirada no bundle)
                        // sub -> root
                        subCa.verify(root.getPublicKey());

                        // Encadeamento consistente
                        if (!authLeaf.getIssuerX500Principal().equals(subCa.getSubjectX500Principal())) continue;
                        if (!subCa.getIssuerX500Principal().equals(root.getSubjectX500Principal())) continue;

                        return new Chain(authLeaf, subCa, root);
                    } catch (Exception e) {
                        last = e; // root errada, tenta outra
                    }
                }
            } catch (Exception e) {
                last = e; // subca errada, tenta outra
            }
        }

        if (last != null) throw last;
        throw new IllegalStateException("Nao consegui construir/validar a cadeia AUTH->SubCA->Root com o pool disponivel.");
    }
}

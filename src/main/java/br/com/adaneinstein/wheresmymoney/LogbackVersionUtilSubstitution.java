package br.com.adaneinstein.wheresmymoney;

import ch.qos.logback.core.Context;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

// ponytail: no native image o logback não resolve sua própria versão (module/manifest = null),
// gerando um WarnStatus que LogbackServiceProvider imprime ignorando status-listener. A checagem
// de versão é inútil num binário estático, então a anulamos. Vale só na compilação nativa.
@TargetClass(className = "ch.qos.logback.core.util.VersionUtil")
final class Target_VersionUtil {

    @Substitute
    public static void checkForVersionEquality(Context context, String s1, String s2, String s3, String s4) {
        // no-op
    }
}

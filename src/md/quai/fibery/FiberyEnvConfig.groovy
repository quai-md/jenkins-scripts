package md.quai.fibery

public class FiberyEnvConfig {
    Closure<String> resolveTaskState;
    Closure<Boolean> validateTask;
    Closure<Boolean> shouldPromote;
}
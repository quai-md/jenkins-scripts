package md.quai.fibery

public class FiberyEnvConfig {
    Closure<String> resolveTaskState;
    Closure<Boolean> validateTask;
    Closure<Boolean> shouldPromote;
    Closure<String> resolveStateName;

    FiberyEnvConfig(Closure<String> resolveTaskState,
                    Closure<Boolean> validateTask,
                    Closure<Boolean> shouldPromote,
                    Closure<String> resolveStateName){
    this.resolveTaskState = resolveTaskState;
    this.validateTask = validateTask;
    this.shouldPromote = shouldPromote;
    this.resolveStateName = resolveStateName;
    }
}
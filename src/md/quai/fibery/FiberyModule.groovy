package md.quai.fibery

import com.nu.art.modular.core.Module
import md.quai.fibery.FiberyTransaction
import com.nu.art.pipeline.workflow.variables.Var_Env


public class FiberyModule extends Module {
    private String token;
    public String env;
    def envProjects = [:]

    public void declareConfig(String env, FiberyEnvConfig config) {
        envProjects[env] = config;
    }

    @Override
    protected void init() {}

    public void setToken(String token) {
        this.logInfo("############# HERE - token #############")
        this.token = token;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String[] promoteTasks(String[] taskPublicIds) {
        this.logInfo('############# HERE - promoteTasks #############')
        FiberyTransaction transaction = new FiberyTransaction(token, this.env);
        transaction.queryTasks(taskPublicIds);
        return taskPublicIds;
    }
}

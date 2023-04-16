package md.quai.fibery

import com.nu.art.modular.core.Module
import md.quai.fibery.FiberyTransaction
import com.nu.art.pipeline.modules.SlackModule
import com.nu.art.pipeline.exceptions.BadImplementationException

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
        this.token = token;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    private List<Object> validateTasks(List<Object> tasks) {
        List<Object> nonPassingTasks = new ArrayList<Object>()
        tasks.each { task ->
            if (!this.envProjects[this.env].validateTask.call(task))
                nonPassingTasks.add(task)
        }
        return nonPassingTasks
    }

    public void promoteTasks(String[] taskPublicIds) {
        def Slack = getModule(SlackModule.class)
        FiberyTransaction transaction = new FiberyTransaction(token, this.env);
        List<Object> tasks = transaction.queryTasks(taskPublicIds);
        List<Object> nonPassingTasks = this.validateTasks(tasks)

        //If any invalid tasks
        if (nonPassingTasks.size() > 0) {
            String message = "Task Promotion - Failed:\n"
            String error = "Failed promoting tasks:\n"

            nonPassingTasks.each { task ->
                message += this.generateTaskSlackMessage(task)
                error += "${task["fibery/public-id"]}\n"
            }

            Slack.notify(message, "#FF0000", "__web-lifecycle")
            throw new BadImplementationException(error);
        }

        transaction.promoteTasks(this.envProjects[this.env].resolveTaskState, tasks)
        String message = "Task Promotion - Success:\n"
        tasks.each { task ->
            message += this.generateTaskSlackMessage(task)
        }
        Slack.notify(message, "#00FF00", "__web-lifecycle")
    }

    private String generateTaskSlackMessage(Object task) {
        return "- <https://quai.fibery.io/Main/Task/${task["fibery/public-id"]}|${task["fibery/public-id"]}> - ${task["Main/Name"]}\n"
    }
}

package md.quai.fibery

import com.nu.art.core.tools.StreamTools
import com.nu.art.http.Transaction_JSON
import com.nu.art.http.consts.HttpMethod
import com.nu.art.pipeline.exceptions.BadImplementationException
import groovy.json.JsonSlurper
import md.quai.fibery.FiberyModule

import java.util.List

public class FiberyTransaction extends Transaction_JSON {

    //######################## Params ########################

    private final String token;
    private FiberyEnvConfig config;

    //######################## Init ########################

    public FiberyTransaction(String token, String env) {
        this.token = token;
        this.config = getModule(FiberyModule.class).envProjects[env];
    }

    //######################## Functionality ########################

    public List<String> queryTasks(String[] taskPublicIds) {
        this.logInfo('############# HERE - queryTasks #############')
        def queryBody = [[
                                 command: "fibery.entity/query",
                                 args   : [
                                         query : [
                                                 "q/from"  : "Main/Task",
                                                 "q/where" : ["q/in", "fibery/public-id", "\$publicIds"],
                                                 "q/limit" : "q/no-limit",
                                                 "q/select": ["fibery/id", "fibery/public-id", "Main/Name", ["workflow/state": ["enum/name", "fibery/id"]]]
                                         ],
                                         params: [
                                                 $publicIds: taskPublicIds
                                         ]
                                 ]
                         ]]
        String URL = "https://quai.fibery.io/api/commands";
        def stream = createRequest()
                .setMethod(HttpMethod.Post)
                .setUrl(URL)
                .addHeader("Authorization", "Token " + token)
                .setBody(gson.toJson(queryBody))
                .executeSync()

        def responseAsString = StreamTools.readFullyAsString(stream);

        this.logInfo("############# HERE - response as string #############")
        this.logInfo(responseAsString);

        this.logInfo("############# HERE - Task Validation #############");

        def data = new JsonSlurper().parseText(responseAsString);

        List<String> successfulTasks = new ArrayList<String>();
        List<String> nonPassingTaskIds = new ArrayList<String>();

        data.each { query ->
            query.result.each { result ->
                if (!this.config.validateTask.call(result))
                    nonPassingTaskIds.add(result["fibery/public-id"]);
                else
                    successfulTasks.add(this.generateTaskSlackMessage(result))
            }
        }

        if (nonPassingTaskIds.size() > 0) {
            String error = String.format("Tasks with invalid states: %s", String.join(",", nonPassingTaskIds));
            throw new BadImplementationException(error);
        }

        this.logInfo("Task Validation - All tasks are okay!");

        this.logInfo("############# HERE - Task Promotion #############");
        def updateBody = [];

        data.each { query ->
            query.result.each { result ->
                updateBody.add(generateTaskPromotionQuery(result))
            }
        }

        def updateStream = createRequest()
                .setMethod(HttpMethod.Post)
                .setUrl(URL)
                .addHeader("Authorization", "Token " + token)
                .setBody(gson.toJson(updateBody))
                .executeSync()

        def updateResponseAsString = StreamTools.readFullyAsString(updateStream);
        this.logInfo("############# HERE - update response as string #############")
        this.logInfo(updateResponseAsString);
        return successfulTasks;
    }

    private String generateTaskSlackMessage(def task) {
        return "- <\"https://quai.fibery.io/Main/Task/${task["fibery/public-id"]}\"|${task["fibery/public-id"]}> - ${task["Main/Name"]}"
    }

    private LinkedHashMap<String, Object> generateTaskPromotionQuery(def task) {
        String promoteId = this.config.resolveTaskState.call(task)
        return [
                command: "fibery.entity/update",
                args   : [
                        "type"  : "Main/Task",
                        "entity": [
                                "fibery/id"     : task["fibery/id"],
                                "workflow/state": ["fibery/id": promoteId]
                        ]
                ]
        ]
    }
}

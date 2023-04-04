package md.quai.fibery;

import com.nu.art.modular.core.Module

@Grab('com.google.code.gson:gson:2.7+')

public class FiberyModule extends Module {
    public static final String readyForDevId = "830ad041-f267-11ec-8e36-eb41e3940f9d";
    public static final String devDoneId = "939a50c1-f267-11ec-8e36-eb41e3940f9d";
    public static final String inProgressId = "932ca1ec-cf29-4a73-b062-c917d8cde832";
    public static final String toValidateInStg = "95c0da42-f267-11ec-8e36-eb41e3940f9d";
    public static final String validatedInStg = "6464ad60-31b3-11ed-a2a4-9520bd30202b";
    public static final String toValidateInProd = "6f480011-31b3-11ed-a2a4-9520bd30202b";

    private String token;

    @Override
    protected void init() {}

    public void setToken(String token) {
        this.token = token;
    }

    public String[] promoteTasks(String[] taskPublicIds) {
        FiberyTransaction transaction = new FiberyTransaction(token);
        transaction.queryTasks(taskPublicIds);
        return taskPublicIds;
    }
}

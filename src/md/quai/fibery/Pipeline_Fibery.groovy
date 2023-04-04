package md.quai.fibery

import com.nu.art.http.HttpModule
import com.nu.art.pipeline.workflow.BasePipeline
import md.quai.fibery.FiberyModule

@Grab('com.nu-art-software:http-module:+')

abstract class Pipeline_Fibery<T extends Pipeline_Fibery>
        extends BasePipeline<Pipeline_Fibery> {

    private static Class<? extends WorkflowModule>[] defaultModules = [FiberyModule.class, HttpModule.class]

    Pipeline_Fibery(String name, Class<? extends WorkflowModule>... modules) {
        super(name, defaultModules + modules)
    }
}
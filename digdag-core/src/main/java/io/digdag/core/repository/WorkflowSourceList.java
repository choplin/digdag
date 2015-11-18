package io.digdag.core.repository;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.google.common.collect.ImmutableList;
import io.digdag.core.config.Config;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.immutables.value.Value;

@Value.Immutable
public abstract class WorkflowSourceList
{
    public abstract List<WorkflowSource> get();

    public static WorkflowSourceList of(List<WorkflowSource> list)
    {
        return ImmutableWorkflowSourceList.builder().addAllGet(list).build();
    }

    @JsonCreator
    public static WorkflowSourceList of(LinkedHashMap<String, Config> map)
    {
        ImmutableList.Builder<WorkflowSource> builder = ImmutableList.builder();
        for (Map.Entry<String, Config> pair : map.entrySet()) {
            builder.add(WorkflowSource.of(pair.getKey(), pair.getValue()));
        }
        return of(builder.build());
    }

    @JsonValue
    public Map<String, Config> toJson()
    {
        Map<String, Config> map = new LinkedHashMap<String, Config>();
        for (WorkflowSource wf : get()) {
            map.put(wf.getName(), wf.getConfig());
        }
        return map;
    }
}
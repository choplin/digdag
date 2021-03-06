package io.digdag.core.repository;

import java.util.List;
import java.time.ZoneId;
import com.google.common.base.Optional;
import io.digdag.core.schedule.Schedule;

public interface ProjectControlStore
{
    StoredRevision insertRevision(int projId, Revision revision)
        throws ResourceConflictException;

    void insertRevisionArchiveData(int revId, byte[] data)
            throws ResourceConflictException;

    StoredWorkflowDefinition insertWorkflowDefinition(int projId, int revId, WorkflowDefinition workflow, ZoneId workflowTimeZone)
        throws ResourceConflictException;

    void updateSchedules(int projId, List<Schedule> schedules)
        throws ResourceConflictException;

    void deleteSchedules(int projId);
}

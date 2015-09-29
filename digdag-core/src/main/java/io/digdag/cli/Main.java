package io.digdag.cli;

import java.io.File;
import java.util.stream.Collectors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.util.List;
import com.google.common.base.*;
import com.google.common.collect.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.digdag.core.*;

public class Main
{
    public static void main(String[] args)
            throws Exception
    {
        Injector injector = Guice.createInjector(
                new ObjectMapperModule()
                    .registerModule(new GuavaModule())
                    .registerModule(new JodaModule()),
                    new DatabaseModule(DatabaseStoreConfig.builder()
                        .type("h2")
                        //.url("jdbc:h2:../test")
                        .url("jdbc:h2:mem:test")
                        .build())
                );

        final ConfigSourceFactory cf = injector.getInstance(ConfigSourceFactory.class);
        final YamlConfigLoader loader = injector.getInstance(YamlConfigLoader.class);
        final WorkflowCompiler compiler = injector.getInstance(WorkflowCompiler.class);
        final RepositoryStore repoStore = injector.getInstance(DatabaseRepositoryStoreManager.class).getRepositoryStore(0);
        final SessionStore sessionStore = injector.getInstance(SessionStoreManager.class).getSessionStore(0);

        injector.getInstance(DatabaseMigrator.class).migrate();

        final ConfigSource ast = loader.loadFile(new File("../demo.yml"));
        List<WorkflowSource> workflowSources = ast.getKeys()
            .stream()
            .map(key -> WorkflowSource.of(key, ast.getNested(key)))
            .collect(Collectors.toList());

        final StoredRepository repo = repoStore.putRepository(
                Repository.of("repo1", cf.create()));
        final StoredRevision rev = repoStore.putRevision(
                repo.getId(),
                Revision.revisionBuilder()
                    .name("rev1")
                    .archiveType("db")
                    .globalParams(cf.create())
                    .build()
                );

        List<StoredWorkflowSource> storedWorkflows = workflowSources
            .stream()
            .map(workflowSource -> repoStore.putWorkflow(rev.getId(), workflowSource))
            .collect(Collectors.toList());

        List<Workflow> workflows = storedWorkflows
            .stream()
            .map(storedWorkflow -> compiler.compile(storedWorkflow.getName(), storedWorkflow.getConfig()))
            .collect(Collectors.toList());

        List<StoredSession> sessions = sessionStore.transaction(() ->
            storedWorkflows
                .stream()
                .map(storedWorkflow -> {
                    System.out.println("Starting a session of workflow "+storedWorkflow);

                    StoredSession s = sessionStore.putSession(
                            Session.sessionBuilder()
                                .name("ses1")
                                .sessionParams(cf.create())
                                .build(),
                            SessionRelation
                                .of(Optional.of(repo.getId()), Optional.of(storedWorkflow.getId())));
                    return s;
                })
                .collect(Collectors.toList())
        );
    }
}
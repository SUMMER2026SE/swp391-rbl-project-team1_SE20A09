package com.sportvenue.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import java.io.File;

public class CleanupOldMigration implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            File oldMigrationSrc = new File("src/main/resources/db/migration/V10__add_private_chat_nicknames.sql");
            if (oldMigrationSrc.exists()) {
                oldMigrationSrc.delete();
            }
            File oldMigrationTarget = new File("target/classes/db/migration/V10__add_private_chat_nicknames.sql");
            if (oldMigrationTarget.exists()) {
                oldMigrationTarget.delete();
            }

            File v11Src = new File("src/main/resources/db/migration/V11__add_private_chat_nicknames.sql");
            if (v11Src.exists()) {
                v11Src.delete();
            }
            File v11Target = new File("target/classes/db/migration/V11__add_private_chat_nicknames.sql");
            if (v11Target.exists()) {
                v11Target.delete();
            }

            File v12Src = new File("src/main/resources/db/migration/V12__add_private_chat_nicknames.sql");
            if (v12Src.exists()) {
                v12Src.delete();
            }
            File v12Target = new File("target/classes/db/migration/V12__add_private_chat_nicknames.sql");
            if (v12Target.exists()) {
                v12Target.delete();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}

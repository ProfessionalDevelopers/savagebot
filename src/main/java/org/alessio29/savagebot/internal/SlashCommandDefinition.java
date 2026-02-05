package org.alessio29.savagebot.internal;

import java.util.List;

public class SlashCommandDefinition {

    public enum OptionType {
        STRING
    }

    public static class Option {
        private final String name;
        private final String description;
        private final OptionType type;
        private final boolean required;

        public Option(String name, String description, OptionType type, boolean required) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public OptionType getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }
    }

    private final String name;
    private final String description;
    private final List<Option> options;
    private final boolean shouldDefer;
    private final String varargOptionName;
    private final String varargOptionDescription;

    public SlashCommandDefinition(
            String name,
            String description,
            List<Option> options,
            boolean shouldDefer,
            String varargOptionName,
            String varargOptionDescription
    ) {
        this.name = name;
        this.description = description;
        this.options = options;
        this.shouldDefer = shouldDefer;
        this.varargOptionName = varargOptionName;
        this.varargOptionDescription = varargOptionDescription;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Option> getOptions() {
        return options;
    }

    public boolean isShouldDefer() {
        return shouldDefer;
    }

    public String getVarargOptionName() {
        return varargOptionName;
    }

    public String getVarargOptionDescription() {
        return varargOptionDescription;
    }
}

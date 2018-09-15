package data.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Data
@AllArgsConstructor
public class Command {
    private ActionType action;
    private String parameter;

    public Command(final Command otherCommand) {
        this.action = otherCommand.action;
        this.parameter = otherCommand.parameter;
    }

    @Override
    public int hashCode() {
        int hashcode = new HashCodeBuilder().append(action).append(parameter).toHashCode();
        return hashcode;
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof Command)) {
            return false;
        }
        final Command otherCommand = (Command) object;

        return this.hashCode() == otherCommand.hashCode();
    }

    @Override
    public String toString() {
        return (action + " " + parameter).trim();
    }
}

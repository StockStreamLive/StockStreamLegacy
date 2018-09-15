package data.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Data
@AllArgsConstructor
public class RankedCommand {
    private Command command;
    private Integer votes;

    public RankedCommand(final RankedCommand otherRankedCommand) {
        this.votes = otherRankedCommand.votes;
        this.command = new Command(otherRankedCommand.getCommand());
    }

    @Override
    public int hashCode() {
        int hashcode = new HashCodeBuilder().append(command).toHashCode();
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
}
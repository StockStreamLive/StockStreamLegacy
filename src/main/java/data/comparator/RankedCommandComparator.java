package data.comparator;

import data.command.RankedCommand;
import java.util.Comparator;

public class RankedCommandComparator implements Comparator<RankedCommand> {

    @Override
    public int compare(final RankedCommand c1, final RankedCommand c2) {
        if (c1.getCommand().equals(c2.getCommand())) {
            return 0;
        }

        if (c2.getVotes() > c1.getVotes()) {
            return 1;
        } else {
            return -1;
        }
    }

}
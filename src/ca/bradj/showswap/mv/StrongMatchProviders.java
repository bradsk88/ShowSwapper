package ca.bradj.showswap.mv;

import ca.bradj.common.base.Failable;
import ca.bradj.gsmatch.Match;
import ca.bradj.gsmatch.Matching;

public class StrongMatchProviders {

    protected static final Failable<Match> NOTHING_TO_MATCH = Failable.fail("There are no names against which to match");
    private static final StrongMatchProvider NO_STRONG_MATCHES = new StrongMatchProvider() {

        @Override
        public Failable<Match> getStrongestMatch(String name) {
            return NOTHING_TO_MATCH;
        }
    };

    public static StrongMatchProvider fromList(final Iterable<String> showNames) {
        return new StrongMatchProvider() {

            @Override
            public Failable<Match> getStrongestMatch(String name) {
                return Matching.getStrongestMatch(showNames, name);
            }
        };
    }

    public static StrongMatchProvider empty() {
        return NO_STRONG_MATCHES;
    }

}

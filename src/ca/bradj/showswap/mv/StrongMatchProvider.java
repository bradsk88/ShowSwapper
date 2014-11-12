package ca.bradj.showswap.mv;

import ca.bradj.common.base.Failable;
import ca.bradj.gsmatch.Match;

public interface StrongMatchProvider {

    Failable<Match> getStrongestMatch(String name);

}

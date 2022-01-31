package meta.works.zim.annotationhelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * In case an interface does not report position (i.e. spotify), this class intends to track play/pause/track-change
 * events to artificially reconstruct the current position. This might kinda work as long as you don't seek/skip/fast-
 * forward/rewind, but is inherently an inaccurate extrapolation and relies heavily on getting regular and prompt
 * state change information.
 */
public
class PositionApproximator
{
    /**
     * This would probably be the number of shows deep you might go back to multiplied by the number of adverts in each
     * of those shows.
     */
    private static final int KEEP = 20;

    private static final
    Logger log = LoggerFactory.getLogger(PositionApproximator.class);

    private static
    class SavedState
    {
        String url;
        long accumulatedMiillis;
        long lastReport;

        @Override
        public
        String toString()
        {
            return "SavedState{" +
                   "url='" + url + '\'' +
                   ", millis=" + accumulatedMiillis +
                   '}';
        }
    }

    private final
    List<SavedState> savedStates = new LinkedList<>();

    public
    long onStateChange(StateSnapshot was, StateSnapshot now)
    {
        if (now.playState != PlayState.Playing)
        {
            log.trace("not accumulating time while {}", now.playState);
            return getState(now.url).accumulatedMiillis;
        }

        if (was.playState != PlayState.Playing || !was.refersToSameContentAs(now))
        {
            log.debug("Ignoring state change report: {} -> {}", was, now);
            return getState(now.url).accumulatedMiillis;
        }

        if (now.refersToSameContentAs(was))
        {
            var millis = continueTracking(was, now);
            log.trace("continue: {} @ {}ms", now, millis);
            return millis;
        }
        else
        {
            log.debug("transition: {} -> {}", was, now);
            stopTracking(was, now.time);
            startTracking(now);
            cacheLimiter();
            return 0L;
        }
    }

    private
    void stopTracking(final StateSnapshot oldState, final long newLaggyReportTime)
    {
        var state = getState(oldState.url);
        var halfDelta =  (newLaggyReportTime - state.lastReport)/2;
        var midpoint = state.lastReport + halfDelta;
        state.lastReport = midpoint;
    }

    private
    void startTracking(final StateSnapshot state)
    {
        getState(state.url).lastReport = state.time;
    }

    private
    long continueTracking(final StateSnapshot was, final StateSnapshot now)
    {
        var url = now.url;

        if (url == null)
        {
            log.debug("unable to continue tracking without a URL");
            return 0;
        }

        var state = getState(url);
        var delta = now.time - was.time;
        state.accumulatedMiillis += delta;
        return state.accumulatedMiillis;
    }

    private
    SavedState getState(final String url)
    {
        for (SavedState savedState : savedStates)
        {
            if (url.equals(savedState.url))
            {
                return savedState;
            }
        }

        log.debug("Creating saved state for: {}", url);

        var state = new SavedState();
        state.url = url;
        savedStates.add(state);
        return state;
    }

    private
    void cacheLimiter()
    {
        while (savedStates.size() > KEEP)
        {
            var state = savedStates.remove(0);
            log.debug("discarding old state: {}", state);
        }
    }
}

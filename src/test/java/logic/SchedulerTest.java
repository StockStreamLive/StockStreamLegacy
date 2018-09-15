package logic;


import data.Event;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SchedulerTest {

    private Scheduler scheduler;

    @Before
    public void setupTest() {
        scheduler = new Scheduler();
    }

    @Test
    public void testSchedule_validEventScheduledAndNotified_expectScheduledMethodCalled() {
        final MutableBoolean runnableCalled = new MutableBoolean(false);

        scheduler.scheduleJob(runnableCalled::setTrue, Event.GAME_TICK);
        scheduler.notifyEvent(Event.GAME_TICK);

        assertEquals(true, runnableCalled.booleanValue());
    }

    @Test
    public void testSchedule_validEventScheduledDiffNotified_expectScheduledMethodNotCalled() {
        final MutableBoolean runnableCalled = new MutableBoolean(false);

        scheduler.scheduleJob(runnableCalled::setTrue, Event.ORDER_PLACED);
        scheduler.notifyEvent(Event.GAME_TICK);

        assertEquals(false, runnableCalled.booleanValue());
    }

    @Test
    public void testSchedule_validDateScheduled_expectScheduledMethodCalled() throws InterruptedException {
        final MutableBoolean runnableCalled = new MutableBoolean(false);

        scheduler.scheduleJob(runnableCalled::setTrue, new DateTime().plusMillis(1));
        Thread.sleep(2);
        scheduler.notifyEvent(Event.GAME_TICK);

        assertEquals(true, runnableCalled.booleanValue());
    }

}

/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;
import java.util.LinkedList;

/**
 * TTCN-3 timer
 *
 * TODO: the destructor can be a problem.
 *
 * @author Kristof Szabados
 * @author Farkas Izabella Ingrid
 */
public class TitanTimer {
	public static final TitanTimer testcaseTimer = new TitanTimer("<testcase guard timer>");

	// linked list of running timers
	private static final ThreadLocal<LinkedList<TitanTimer>> TIMERS = new ThreadLocal<LinkedList<TitanTimer>>() {
		@Override
		protected LinkedList<TitanTimer> initialValue() {
			return new LinkedList<TitanTimer>();
		}
	};
	private static final ThreadLocal<LinkedList<TitanTimer>> BACKUP_TIMERS = new ThreadLocal<LinkedList<TitanTimer>>() {
		@Override
		protected LinkedList<TitanTimer> initialValue() {
			return new LinkedList<TitanTimer>();
		}
	};

	private String timerName;
	private boolean hasDefault;
	private boolean isStarted;
	private double defaultValue;
	private double timeStarted;
	private double timeExpires;
	private static boolean controlTimerSaved = false;

	protected TitanTimer(){

	}

	TitanTimer assign(final TitanTimer otherValue) {
		timerName = otherValue.timerName;
		hasDefault = otherValue.hasDefault;
		isStarted = otherValue.isStarted;
		defaultValue = otherValue.defaultValue;
		timeStarted = otherValue.timeStarted;
		timeExpires = otherValue.timeExpires;

		return this;
	}

	public TitanTimer assign(final Ttcn3Float defaultValue) {
		setDefaultDuration(defaultValue);
		isStarted = false;

		return this;
	}

	public TitanTimer assign(final TitanFloat defaultValue) {
		defaultValue.mustBound("Initializing a timer duration with an unbound float value.");

		setDefaultDuration(defaultValue);
		isStarted = false;

		return this;
	}

	public TitanTimer(final String name) {
		if (name == null) {
			timerName = "<unknown>";
		} else {
			timerName = name;
		}
		hasDefault = false;
		isStarted = false;
	}

	public TitanTimer(final String name, final double defaultValue) {
		if (name == null) {
			throw new TtcnError("Internal Error: Creating a timer with an invalid name.");
		}

		timerName = name;
		setDefaultDuration(defaultValue);
		isStarted = false;
	}

	public TitanTimer(final String name, final Ttcn3Float defaultValue) {
		if (name == null) {
			throw new TtcnError("Internal Error: Creating a timer with an invalid name.");
		}

		timerName = name;
		setDefaultDuration(defaultValue);
		isStarted = false;
	}

	public TitanTimer(final String name, final TitanFloat defaultValue) {
		if (name == null) {
			throw new TtcnError("Internal Error: Creating a timer with an invalid name.");
		}
		defaultValue.mustBound("Initializing a timer duration with an unbound float value.");

		timerName = name;
		setDefaultDuration(defaultValue);
		isStarted = false;
	}

	/**
	 * Add the current timer instance to the end of the running timers list.
	 * */
	private void addToList() {
		if (TIMERS.get().contains(this)) {
			return;
		}

		TIMERS.get().addLast(this);
	}

	/**
	 * Remove the current timer from the list of running timers
	 * */
	private void removeFromList() {
		TIMERS.get().remove(this);
	}

	//originally TIMER::set_name
	public void setName(final String name) {
		if (name == null) {
			throw new TtcnError("Internal error: Setting an invalid name for a single element of a timer array.");
		}
		timerName = name;
	}
	// originally set_default_duration
	public final void setDefaultDuration(final double defaultValue) {
		if (defaultValue < 0.0) {
			throw new TtcnError(MessageFormat.format("Setting the default duration of timer {0} to a negative float value ({1}).",
					timerName, defaultValue));
		} else if (Double.isInfinite(defaultValue) || Double.isNaN(defaultValue)) {
			throw new TtcnError(
					MessageFormat.format("Setting the default duration of timer {0} to a non-numeric float value ({1}).",
							timerName, defaultValue));
		}

		hasDefault = true;
		this.defaultValue = defaultValue;
	}

	// originally set_default_duration
	public final void setDefaultDuration(final Ttcn3Float defaultValue) {
		setDefaultDuration(defaultValue.getValue());
	}

	// originally set_default_duration
	public final void setDefaultDuration(final TitanFloat defaultValue) {
		defaultValue.mustBound(MessageFormat.format("Setting the default duration of timer {0} to an unbound float value.", timerName));

		setDefaultDuration(defaultValue.getValue());
	}

	// originally start
	public void start() {
		if (!hasDefault) {
			throw new TtcnError(MessageFormat.format("Timer {0} does not have default duration. It can only be started with a given duration.",
					timerName));
		}

		start(defaultValue);
	}

	// originally start(double start_val)
	public void start(final double startValue) {
		if (this != testcaseTimer) {
			if (startValue < 0.0) {
				throw new TtcnError(MessageFormat.format("Starting timer {0} with a negative duration ({1}).",
						timerName, startValue));
			}
			if (Double.isNaN(startValue) || Double.isInfinite(startValue)) {
				throw new TtcnError(MessageFormat.format("Starting timer {0} with a non-numeric float value ({1}).",
						timerName, startValue));
			}
			if (isStarted) {
				TtcnError.TtcnWarning(MessageFormat.format("Re-starting timer {0}, which is already active (running or expired).",
						timerName));
				removeFromList();
			} else {
				isStarted = true;
			}

			TtcnLogger.log_timer_start(timerName, startValue);
			addToList();
		} else {
			if (startValue < 0.0) {
				throw new TtcnError(MessageFormat.format("Using a negative duration ({0}) for the guard timer of the test case.",
						startValue));
			}
			if (Double.isNaN(startValue) || Double.isInfinite(startValue)) {
				throw new TtcnError(MessageFormat.format("Using a non-numeric float value ({0}) for the guard timer of the test case.",
						startValue));
			}

			isStarted = true;
			TtcnLogger.log_timer_guard(startValue);
		}

		timeStarted = TTCN_Snapshot.timeNow();
		timeExpires = timeStarted + startValue;
	}

	// originally start(const FLOAT& start_val)
	public void start(final Ttcn3Float startValue) {
		start(startValue.getValue());
	}

	// originally start(const FLOAT& start_val)
	public void start(final TitanFloat startValue) {
		startValue.mustBound(MessageFormat.format("Starting timer {0} with an unbound float value as duration.", timerName));

		start(startValue.getValue());
	}

	// originally stop()
	public void stop() {
		if (this != testcaseTimer) {
			if (isStarted) {
				isStarted = false;
				TtcnLogger.log_timer_stop(timerName, timeExpires - timeStarted);
				removeFromList();
			} else {
				TtcnError.TtcnWarning(MessageFormat.format("Stopping inactive timer {0}.", timerName));
			}
		} else {
			isStarted = false;
		}
	}

	/**
	 * originally read
	 *
	 * @return the number of seconds until the timer expires.
	 * */
	public TitanFloat read() {
		double returnValue;

		if (isStarted) {
			final double currentTime = TTCN_Snapshot.timeNow();
			if (currentTime >= timeExpires) {
				returnValue = 0.0;
			} else {
				returnValue = currentTime - timeStarted;
			}
		} else {
			returnValue = 0.0;
		}

		TtcnLogger.log_timer_read(timerName, returnValue);

		return new TitanFloat(returnValue);
	}

	/**
	 * @return true if is_started and not yet expired, false otherwise.
	 *
	 * originally running(Index_Redirect* = null)
	 */
	public boolean running() {
		return running(null);
	}

	/**
	 * @return true if is_started and not yet expired, false otherwise.
	 *
	 * originally running(Index_Redirect*)
	 */
	public boolean running(final Index_Redirect index_redirect) {
		return isStarted && TTCN_Snapshot.timeNow() < timeExpires;
	}

	/**
	 * Return the alt status.
	 *
	 * @return ALT_NO if the timer is not started.
	 * @return ALT_MAYBE if it's started and the snapshot was taken before
	 *         the expiration time
	 * @return ALT_YES if it's started and the snapshot is past the
	 *         expiration time
	 *
	 *         originally timeout(Index_Redirect* = null)
	 * */
	public TitanAlt_Status timeout() {
		return timeout(null);
	}

	/**
	 * Return the alt status.
	 *
	 * @return ALT_NO if the timer is not started.
	 * @return ALT_MAYBE if it's started and the snapshot was taken before
	 *         the expiration time
	 * @return ALT_YES if it's started and the snapshot is past the
	 *         expiration time
	 *
	 *         originally timeout(Index_Redirect*)
	 * */
	public TitanAlt_Status timeout(final Index_Redirect index_redirect) {
		if (isStarted) {
			if (TTCN_Snapshot.getAltBegin() < timeExpires) {
				return TitanAlt_Status.ALT_MAYBE;
			}

			isStarted = false;
			if (this != testcaseTimer) {
				TtcnLogger.log_timer_timeout(timerName, timeExpires - timeStarted);
				removeFromList();
			}

			return TitanAlt_Status.ALT_YES;
		} else {
			if (this != testcaseTimer) {
				TtcnLogger.log_matching_timeout(timerName);
			}

			return TitanAlt_Status.ALT_NO;
		}
	}

	/**
	 * stop all running timers.
	 * (empty the list)
	 * */
	public static void allStop() {
		while (TIMERS.get().size() != 0) {
			TIMERS.get().get(0).stop();
		}
	}

	/**
	 * @return true if there is a running timer.
	 * */
	public static boolean anyRunning() {
		for (final TitanTimer timer : TIMERS.get()) {
			if (timer.running(null)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Return the alt status.
	 *
	 * @return ALT_NO if no timer is running.
	 * @return ALT_MAYBE if there is at least one timer that is started
	 *         and the snapshot was taken before it's expiration time
	 * @return ALT_YES if there is at least one time that is started
	 *         and the snapshot is past it's expiration time
	 *
	 *         originally any_timeout()
	 * */
	public static TitanAlt_Status anyTimeout() {
		TitanAlt_Status returnValue = TitanAlt_Status.ALT_NO;
		for (final TitanTimer timer : TIMERS.get()) {
			switch (timer.timeout(null)) {
			case ALT_YES:
				TtcnLogger.log_timer_any_timeout();
				return TitanAlt_Status.ALT_YES;
			case ALT_MAYBE:
				returnValue = TitanAlt_Status.ALT_MAYBE;
				break;
			default:
				throw new TtcnError(MessageFormat.format("Internal error: Timer {0} returned unexpected status code while evaluating `any timer.timeout'.",
						timer.timerName));
			}
		}

		if (returnValue == TitanAlt_Status.ALT_NO) {
			TtcnLogger.log_matching_timeout(null);
		}

		return returnValue;
	}

	/**
	 * Get the earliest expiration time for the running timers.
	 * Includes the testcase's guard timer.
	 *
	 * @param minValue will return the expiration time if one is found.
	 * @return true if an active timer was found, false otherwise.
	 * */
	public static boolean getMinExpiration(final Changeable_Double minValue) {
		boolean minFlag = false;
		final double altBegin = TTCN_Snapshot.getAltBegin();

		if (testcaseTimer.isStarted && testcaseTimer.timeExpires > altBegin) {
			minValue.setValue(testcaseTimer.timeExpires);
			minFlag = true;
		}

		for (final TitanTimer timer : TIMERS.get()) {
			if (timer.timeExpires < altBegin) {
				//ignore timers that expired before the snapshot
				continue;
			} else if (!minFlag || timer.timeExpires < minValue.getValue()){
				minValue.setValue(timer.timeExpires);
				minFlag = true;
			}
		}


		return minFlag;
	}

	// originally TIMER::save_control_timers
	public static void saveControlTimers() {
		if (controlTimerSaved) {
			throw new TtcnError("Internal error: Control part timers are already saved.");
		}

		if (!TIMERS.get().isEmpty()) {
			BACKUP_TIMERS.get().addAll(TIMERS.get());
			TIMERS.get().clear();
		}
		controlTimerSaved = true;
	}

	//originally TIMER::restore_control_timers
	public static void restore_control_timers() {
		if (!controlTimerSaved) {
			throw new TtcnError("Internal error: Control part timers are not saved.");
		}

		if (!TIMERS.get().isEmpty()) {
			throw new TtcnError("Internal error: There are active timers. Control part timers cannot be restored.");
		}

		if (!BACKUP_TIMERS.get().isEmpty()) {
			TIMERS.get().addAll(BACKUP_TIMERS.get());
			BACKUP_TIMERS.get().clear();
		}
		controlTimerSaved = false;
	}

	// originally TIMER::log()
	public void log() {
		// the time is not frozen (i.e. time_now() is used)
		TtcnLogger.log_event("timer: { name: " + timerName + ", default duration: ");
		if (hasDefault) {
			TtcnLogger.log_event(defaultValue + " s");
		} else {
			TtcnLogger.log_event_str("none");
		}
		TtcnLogger.log_event_str(", state: ");
		if (isStarted) {
			final double current_time = TTCN_Snapshot.timeNow();
			if (current_time < timeExpires) {
				TtcnLogger.log_event_str("running");
			} else {
				TtcnLogger.log_event_str("expired");
			}
			TtcnLogger.log_event(", actual duration: " + (timeExpires - timeStarted) + " s,elapsed time: " + (current_time - timeStarted)
					+ " s");
		} else {
			TtcnLogger.log_event_str("inactive");
		}
		TtcnLogger.log_event_str(" }");
	}
}

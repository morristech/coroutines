//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package de.esoco.coroutine.step;

import de.esoco.coroutine.Continuation;
import de.esoco.coroutine.Coroutine;
import de.esoco.coroutine.CoroutineException;
import de.esoco.coroutine.CoroutineStep;
import de.esoco.coroutine.Suspension;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/********************************************************************
 * A suspending {@link Coroutine} step that pauses execution for an amount of
 * time.
 *
 * @author eso
 */
public class TimerStep<T> extends CoroutineStep<T, T>
{
	//~ Instance fields --------------------------------------------------------

	private int		 nDuration;
	private TimeUnit eTimeUnit;

	//~ Constructors -----------------------------------------------------------

	/***************************************
	 * Creates a new instance.
	 *
	 * @param nDuration The duration to pause
	 * @param eUnit     The time unit of the duration
	 */
	public TimerStep(int nDuration, TimeUnit eUnit)
	{
		if (nDuration < 0)
		{
			throw new IllegalArgumentException("Durations must be >= 0");
		}

		Objects.requireNonNull(eUnit);

		this.nDuration = nDuration;
		this.eTimeUnit = eUnit;
	}

	//~ Static methods ---------------------------------------------------------

	/***************************************
	 * Suspends the coroutine execution for a certain time in milliseconds.
	 *
	 * @param  nMilliseconds The milliseconds to sleep
	 *
	 * @return A new step instance
	 */
	public static <T> TimerStep<T> sleep(int nMilliseconds)
	{
		return sleep(nMilliseconds, TimeUnit.MILLISECONDS);
	}

	/***************************************
	 * Suspends the coroutine execution for a certain time.
	 *
	 * @param  nDuration The duration to sleep
	 * @param  eUnit     The time unit of the duration
	 *
	 * @return A new step instance
	 */
	public static <T> TimerStep<T> sleep(int nDuration, TimeUnit eUnit)
	{
		return new TimerStep<>(nDuration, eUnit);
	}

	//~ Methods ----------------------------------------------------------------

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public T execute(T rInput, Continuation<?> rContinuation)
	{
		try
		{
			eTimeUnit.sleep(nDuration);
		}
		catch (InterruptedException e)
		{
			throw new CoroutineException(e);
		}

		return rInput;
	}

	/***************************************
	 * {@inheritDoc}
	 */
	@Override
	public void runAsync(CompletableFuture<T> fPreviousExecution,
						 CoroutineStep<T, ?>  rNextStep,
						 Continuation<?>	  rContinuation)
	{
		Suspension<T> rSuspension = rContinuation.suspend(rNextStep);

		fPreviousExecution.thenAcceptAsync(
			i ->
		{
			rContinuation.context()
			.getScheduler()
			.schedule(() -> rSuspension.resume(i), nDuration, eTimeUnit);
		},
		rContinuation);
	}
}
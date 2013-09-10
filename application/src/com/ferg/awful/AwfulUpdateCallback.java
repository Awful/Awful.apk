/********************************************************************************
 * Copyright (c) 2012, Matthew Shepard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awful;

import android.os.Message;

import com.ferg.awful.preferences.AwfulPreferences;

public interface AwfulUpdateCallback {
	/**
	 * Called when the loading process for this view has failed.
	 * Keep in mind, the user may still have cached data.
	 */
	public void loadingFailed(Message aMsg);
	/**
	 * Called when a background load for this page has begun.
	 */
	public void loadingStarted(Message aMsg);
	/**
	 * Called when a task sends a status update.
	 */
	public void loadingUpdate(Message aMsg);
	/**
	 * Called when a loading process has succeeded for the current view.
	 * This does not supplement/replace dataUpdate(), it is only used for displaying loading status.
	 */
	public void loadingSucceeded(Message aMsg);
	/**
	 * Called when any preference changes. Use this callback to update text/background color, font sizes, ect.
	 * @param mPrefs 
	 */
	public void onPreferenceChange(AwfulPreferences prefs);
}

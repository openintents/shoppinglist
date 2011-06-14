/* 
 * Copyright (C) 2007-2010 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openintents.shopping;

import android.os.Bundle;
import android.support.v2.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * <pre>
 * This replaces the top spinner with the List at left side for OS3 tablets
 *  
 * @author Temp
 * </pre>
 */
public class ShoppingListFilterOS3 extends ListFragment {

	private ListAdapter mListAdapter;
	private ListView mListView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater
				.inflate(R.layout.shopping_list_filter, container, false);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		updateAdapter(mListAdapter);
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		// Temp- since click delegate would recall the same method, also
		// re-using old OS code
		// setting as tag, so that old code doesn't need to adapt much
		// can be further identified by using key
		mListView.setTag(position);
		mListView.getOnItemSelectedListener()
				.onItemSelected(l, v, position, id);

	}

	/**
	 * As per OS 3 , setListAdapter must be called before setAdapter of the
	 * ListView.
	 * 
	 * Thus this method is being used to store the adapter and manage delayed
	 * calling in comparison to lower version Spinners
	 * 
	 * @param adapter
	 */
	public void setAdapter(ListAdapter adapter) {
		mListAdapter = adapter;
		updateAdapter(adapter);
	}

	/**
	 * updates the associated adapter with the fragment based upon the changes
	 * performed in the activity
	 * 
	 * @param adapter
	 */

	private void updateAdapter(ListAdapter adapter) {
		if (mListAdapter != null) {
			setListAdapter(mListAdapter);
			mListView = ((ListView) (getActivity()
					.findViewById(android.R.id.list)));
			mListView.setAdapter(mListAdapter);
		}
	}

}

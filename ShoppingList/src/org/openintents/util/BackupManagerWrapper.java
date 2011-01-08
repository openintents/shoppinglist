package org.openintents.util;

import android.app.backup.BackupManager;
import android.content.Context;

public class BackupManagerWrapper {
	private BackupManager mInstance;
	
	 /* class initialization fails when this throws an exception */
	   static {
	       try {
	           Class.forName("android.app.backup.BackupManager");
	       } catch (Exception ex) {
	           throw new RuntimeException(ex);
	       }
	   }	   

	   /* calling here forces class initialization */
	   public static void checkAvailable() {}


	   public BackupManagerWrapper(Context ctx) {
		   mInstance = new BackupManager(ctx);
		   
	   }
	   
	   public void dataChanged(){
		   mInstance.dataChanged();
	}
}

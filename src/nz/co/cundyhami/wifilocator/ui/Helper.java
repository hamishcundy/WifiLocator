package nz.co.cundyhami.wifilocator.ui;

public class Helper {
	
	//This method sourced from http://rvmiller.com/2013/05/part-1-wifi-based-trilateration-on-android/
	
	/**Calculates the estimated distance between the access point based on frequency and signal strength in db. 
	 * Based on the Free-space path loss equation at http://en.wikipedia.org/wiki/FSPL
	 * 
	 * @param levelInDb
	 * @param freqInMHz
	 * @return
	 */
		public static double calculateDistance(double levelInDb, double freqInMHz)    {
			   double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(levelInDb)) / 20.0;
			   return Math.pow(10.0, exp);
			}

}

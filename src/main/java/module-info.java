module gg.solarmc.futuresfactory {

	requires space.arim.managedwaits;
	/*
	 * TODO: Fix this and change to com.lmax.disruptor
	 * disruptor 3.4.2 from April 2018 does not have Automatic-Module-Name
	 * disruptor 4.x will be a full module
	 */
	requires disruptor;
}
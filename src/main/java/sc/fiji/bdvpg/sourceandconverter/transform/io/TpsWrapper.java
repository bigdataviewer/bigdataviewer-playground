package sc.fiji.bdvpg.sourceandconverter.transform.io;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;

public class TpsWrapper
{
	private ThinPlateR2LogRSplineKernelTransform tps;

	public TpsWrapper(){ }

	public TpsWrapper( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		set( tps );
	}
	
	public void set( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		this.tps = tps;
	}

	public ThinPlateR2LogRSplineKernelTransform get()
	{
		return tps;
	}
}

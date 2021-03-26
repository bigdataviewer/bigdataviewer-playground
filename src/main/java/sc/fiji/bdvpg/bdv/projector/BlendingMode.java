package sc.fiji.bdvpg.bdv.projector;

import com.google.gson.annotations.SerializedName;

public enum BlendingMode
{
	@SerializedName("sum")
	Sum,
	@SerializedName("sumOccluding")
	SumOccluding,
	@SerializedName("average")
	Average,
	@SerializedName("averageOccluding")
	AverageOccluding;

	// To use in Commands until they can do enums as choices
	public static final String AVERAGE = "Average";
	public static final String SUM = "Sum";

	// To use as a key for the xml
	// underscore necessary for valid xml element to store in @see DisplaySettings
	public static final String BLENDING_MODE = "Blending Mode";

	public static boolean isOccluding( BlendingMode blendingMode )
	{
		return ( blendingMode.equals( BlendingMode.AverageOccluding ) || blendingMode.equals( BlendingMode.SumOccluding ) );
	}
}

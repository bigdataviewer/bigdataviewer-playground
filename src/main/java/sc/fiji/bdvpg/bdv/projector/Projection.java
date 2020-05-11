package sc.fiji.bdvpg.bdv.projector;

/**
 * Constants to define the projection mode of sources.
 * These constants are used within {@link AccumulateMixedProjectorARGB}.
 *
 * PROJECTION_MODE_SUM
 * The ARGB values of the source will be added to the final ARGB to be displayed.
 *
 * PROJECTION_MODE_AVG
 * The ARGB values of all sources with the Average projection mode will first be averaged
 * before being added to the final ARGB to be displayed.
 * This is useful for overlapping electron microscopy data sets.
 *
 * PROJECTION_MODE_EXCLUSIVE
 * For a given pixel, if there are sources with the Exclusive projection mode and with
 * an alpha value larger than zero, only these source will be displayed.
 * The pixels of all other sources will not be visible.
 * This is useful, e.g., if there is a region where one source contains information
 * at a higher resolution than another source. Selecting the Exclusive projection mode
 * can be used to only show this source.
 *
 */
public class Projection
{
	public static final String PROJECTION_MODE = "Projection Mode"; // underscore necessary for valid xml element to store in @see DisplaySettings
	public static final String PROJECTION_MODE_XML = "Projection_Mode";// underscore necessary for valid xml element to store in @see DisplaySettings
	public static final String PROJECTION_MODE_SUM = "Sum";
	public static final String PROJECTION_MODE_AVG = "Average";
	public static final String PROJECTION_MODE_OCCLUDING = "Occluding";

	public static final String PROJECTOR = "Projector";
	public static final String MIXED_PROJECTOR = "Mixed Projector";
	public static final String SUM_PROJECTOR = "Sum Projector";
	public static final String AVERAGE_PROJECTOR = "Average Projector";
}

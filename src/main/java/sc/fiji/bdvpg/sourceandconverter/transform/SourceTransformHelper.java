package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.AbstractSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import java.lang.reflect.Method;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;


/**
 * Helper class that helps to apply an affinetransform to a {@link SourceAndConverter}
 *
 * Because there are many ways the affinetransform can be applied to a source depending
 * on the the spimsource class and on how you want to deal with the previous already existing
 * transforms
 */


public class SourceTransformHelper {
    /**
     *
     * branch between mutateTransformedSourceAndConverter and mutateLastSpimdataTransformation depending  on the source class
     *
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter mutate(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        if (sacTR.sac.getSpimSource() instanceof AbstractSpimSource) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sacTR.sac,SPIM_DATA_INFO)!=null) {
                return mutateLastSpimdataTransformation(affineTransform3D, sacTR);
            } else {
                if (sacTR.sac.getSpimSource() instanceof TransformedSource) {
                    return mutateTransformedSourceAndConverter(affineTransform3D,sacTR);
                } else {
                    return createNewTransformedSourceAndConverter(affineTransform3D,sacTR);
                }
            }
        } else if (sacTR.sac.getSpimSource() instanceof TransformedSource) {
            return mutateTransformedSourceAndConverter(affineTransform3D,sacTR);
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sacTR);
        }
    }

    /**
     *  branch between createNewTransformedSourceAndConverter and appendNewSpimdataTransformation depending on the source class
     *
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter append(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        if (sacTR.sac.getSpimSource() instanceof AbstractSpimSource) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sacTR.sac,SPIM_DATA_INFO)!=null) {
                return appendNewSpimdataTransformation(affineTransform3D, sacTR);
            } else {
                return createNewTransformedSourceAndConverter(affineTransform3D,sacTR);
            }
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sacTR);
        }
    }

    /**
     *
     * branch between setTransformedSourceAndConverter and setLastSpimdataTransformation depending on the source class
     *
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter set(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        if (sacTR.sac.getSpimSource() instanceof AbstractSpimSource) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sacTR.sac,SPIM_DATA_INFO)!=null) {
                return setLastSpimdataTransformation(affineTransform3D, sacTR);
            } else {
                if (sacTR.sac.getSpimSource() instanceof TransformedSource) {
                    return setTransformedSourceAndConverter(affineTransform3D,sacTR);
                } else {
                    return createNewTransformedSourceAndConverter(affineTransform3D,sacTR);
                }
            }
        } else if (sacTR.sac.getSpimSource() instanceof TransformedSource) {
            return setTransformedSourceAndConverter(affineTransform3D,sacTR);
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sacTR);
        }
    }

    /**
     * Ignores registration
     * @param affineTransform3D
     * @return
     */
    public static SourceAndConverter cancel(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        return sacTR.sac;
    }

    /**
     * if a source has a linked spimdata, mutates the last registration to account for changes
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter mutateLastSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sacTR.sac).containsKey(SPIM_DATA_INFO);
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sacTR.sac).get(SPIM_DATA_INFO) instanceof SourceAndConverterService.SpimDataInfo;

        SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)
                SourceAndConverterServices.getSourceAndConverterService()
                        .getSacToMetadata().get(sacTR.sac).get(SPIM_DATA_INFO));

        sacTR.getTimePoints().forEach( timePoint -> {
            ViewRegistration vr = sdi.asd.getViewRegistrations().getViewRegistration(timePoint, sdi.setupId);

            ViewTransform vt = vr.getTransformList().get(vr.getTransformList().size() - 1);

            AffineTransform3D at3D = new AffineTransform3D();
            at3D.concatenate(vt.asAffine3D());
            at3D.preConcatenate(affineTransform3D);

            ViewTransform newvt = new ViewTransformAffine(vt.getName(), at3D);

            vr.getTransformList().remove(vt);
            vr.getTransformList().add(newvt);
            vr.updateModel();


            try {
                Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
                updateBdvSource.setAccessible(true);
                AbstractSpimSource ass = (AbstractSpimSource) sacTR.sac.getSpimSource();
                updateBdvSource.invoke(ass, timePoint);

                if (sacTR.sac.asVolatile() != null) {
                    ass = (AbstractSpimSource) sacTR.sac.asVolatile().getSpimSource();
                    updateBdvSource.invoke(ass, timePoint);
                }

            } catch (ClassCastException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return sacTR.sac;
    }

    /**
     * if a source has a linked spimdata, mutates the last registration to account for changes
     *
     * contrary to mutate, the previous transform is erased and not preconcatenates
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter setLastSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sacTR.sac).containsKey(SPIM_DATA_INFO);
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sacTR.sac).get(SPIM_DATA_INFO) instanceof SourceAndConverterService.SpimDataInfo;

        SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)
                SourceAndConverterServices.getSourceAndConverterService()
                        .getSacToMetadata().get(sacTR.sac).get(SPIM_DATA_INFO));

        sacTR.getTimePoints().forEach( timePoint -> {
            ViewRegistration vr = sdi.asd.getViewRegistrations().getViewRegistration(timePoint, sdi.setupId);

            ViewTransform vt = vr.getTransformList().get(vr.getTransformList().size() - 1);

            ViewTransform newvt = new ViewTransformAffine(vt.getName(), affineTransform3D);

            vr.getTransformList().remove(vt);
            vr.getTransformList().add(newvt);
            vr.updateModel();

            try {
                Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
                updateBdvSource.setAccessible(true);
                AbstractSpimSource ass = (AbstractSpimSource) sacTR.sac.getSpimSource();
                updateBdvSource.invoke(ass, timePoint);

                if (sacTR.sac.asVolatile() != null) {
                    ass = (AbstractSpimSource) sacTR.sac.asVolatile().getSpimSource();
                    updateBdvSource.invoke(ass, timePoint);
                }

            } catch (ClassCastException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return sacTR.sac;
    }

    /**
     * if a source has a linked spimdata, appends a new transformation in the registration model
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter appendNewSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sacTR.sac).containsKey(SPIM_DATA_INFO);
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sacTR.sac).get(SPIM_DATA_INFO) instanceof SourceAndConverterService.SpimDataInfo;

        SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)
                SourceAndConverterServices.getSourceAndConverterService()
                        .getSacToMetadata().get(sacTR.sac).get(SPIM_DATA_INFO));

        ViewTransform newvt = new ViewTransformAffine("Manual transform", affineTransform3D);

        sacTR.getTimePoints().forEach( timePoint -> {
            sdi.asd.getViewRegistrations().getViewRegistration(timePoint, sdi.setupId).preconcatenateTransform(newvt);
            sdi.asd.getViewRegistrations().getViewRegistration(timePoint, sdi.setupId).updateModel();

            try {
                Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
                updateBdvSource.setAccessible(true);
                AbstractSpimSource ass = (AbstractSpimSource) sacTR.sac.getSpimSource();
                updateBdvSource.invoke(ass, timePoint);

                if (sacTR.sac.asVolatile() != null) {
                    ass = (AbstractSpimSource) sacTR.sac.asVolatile().getSpimSource();
                    updateBdvSource.invoke(ass, timePoint);
                }

            } catch (ClassCastException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return sacTR.sac;
    }

    /**
     * Wraps into transformed sources the registered sources
     * Note : time range is ignored (using TransformedSource)
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter createNewTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        SourceAndConverter transformedSac = new SourceAffineTransformer(sacTR.sac, affineTransform3D).getSourceOut();
        return transformedSac;
    }

    /**
     * provided a source was already a transformed source, updates the inner affineTransform3D
     * Note : timerange ignored
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter mutateTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        assert sacTR.sac.getSpimSource() instanceof TransformedSource;
        AffineTransform3D at3D = new AffineTransform3D();
        ((TransformedSource)sacTR.sac.getSpimSource()).getFixedTransform(at3D);
        ((TransformedSource)sacTR.sac.getSpimSource()).setFixedTransform(at3D.preConcatenate(affineTransform3D));
        return sacTR.sac;
    }

    /**
     * provided a source was already a transformed source, sets the inner affineTransform3D
     * Contrary to mutateTransformedSourceAndConverter, the original transform is not preconcatenated
     * Note : timerange ignored
     * @param affineTransform3D
     * @param sacTR
     * @return
     */
    public static SourceAndConverter setTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverterAndTimeRange sacTR) {
        assert sacTR.sac.getSpimSource() instanceof TransformedSource;
        ((TransformedSource)sacTR.sac.getSpimSource()).setFixedTransform(affineTransform3D);
        return sacTR.sac;
    }

}

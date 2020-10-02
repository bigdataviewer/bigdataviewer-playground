package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

/**
 * Removes an editor behaviour installed
 * See {@link EditorBehaviourInstaller}
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL 2020
 */

public class EditorBehaviourUnInstaller implements Runnable {

    BdvHandle bdvh;

    public EditorBehaviourUnInstaller(BdvHandle bdvh) {
        this.bdvh = bdvh;
    }

    @Override
    public void run() {

        SourceSelectorBehaviour ssb = (SourceSelectorBehaviour) SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplayMetadata(
                bdvh, SourceSelectorBehaviour.class.getSimpleName());

        EditorBehaviourInstaller ebi = (EditorBehaviourInstaller) SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplayMetadata(
                bdvh, EditorBehaviourInstaller.class.getSimpleName());

        if ((ssb==null)||(ebi==null)) {
            System.err.println("SourceSelectorBehaviour or EditorBehaviourInstaller cannot be retrieved. Cannot uninstall EditorBehaviour");
            return;
        }

        ebi.getToggleListener().isDisabled();
        ssb.removeToggleListener(ebi.getToggleListener());

        // Cleans the MetaData hashMap
        SourceAndConverterServices.getSourceAndConverterDisplayService().setDisplayMetadata(
                bdvh, EditorBehaviourInstaller.class.getSimpleName(), null);

    }

}

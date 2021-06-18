/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.spimdata;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.ViewSetup;
import org.scijava.plugin.SciJavaPlugin;

/**
 * Plugins which can handle {@link mpicbg.spim.data.generic.base.Entity} when
 * they are being loaded with a {@link sc.fiji.bdvpg.scijava.services.SourceAndConverterService}
 *
 * When opening a SpimData object:
 * * a SpimDataEntity Handler can be executed to create the SourceAndConverter object
 * from the {@link Source} and and the {@link BasicViewSetup} : see TODO
 * * Or to modify the SourceAndConverter once it has been created (applying display settings)
 *
 * TODO think if it can be useful to handle explicitely the case before the SourceAndConverter is created
 *
 * When saving a SpimData object:
 * * it gets the sourceandconverter which is being saved, and can thus write its associated entity, when
 * being given the BasicViewSetup and the SourceAndConverter
 *
 */

public interface EntityHandler extends SciJavaPlugin {

    /**
     *
     * @return the entity type being handled by this entity handler
     */
    Class<? extends Entity> getEntityType();

    /**
     * @return true if this entity provides a way to create the SourceAndConverter object
     *
     * if true is returned, then makeSourceAndConverter is called to create the SourceAndConverter
     * conflicts may occur!
     */
    default boolean canCreateSourceAndConverter() {return false;}

    /**
     * @param viewSetup currently being stored
     * @param sac sourceand converter being saved
     * @return true if this entity was written, the entity is not always necessarily written when saving the data
     */
    boolean writeEntity(BasicViewSetup viewSetup, SourceAndConverter<?> sac);

    /**
     * This method is called before the SourceAndConverter object is created
     * @param spimData object which is being opened (at least converted from Source to SourceAndConverter)
     * @param viewSetup viewSetup associated to the Source
     * @return true if this entity was loaded, the entity is not always loaded
     */
    boolean loadEntity(AbstractSpimData spimData, BasicViewSetup viewSetup);

    /**
     * This method is called after the SourceAndConverter object is created
     * @param spimData object which is being opened
     * @param viewSetup viewSetup associated to the sourceandconverter
     * @param sac current sourceandconverter being opened from the spimData object
     * @return true is the entity was loaded
     */
    boolean loadEntity(AbstractSpimData spimData, BasicViewSetup viewSetup, SourceAndConverter<?> sac);

    /**
     * If canCreateSourceAndConverter returns true, this function is called in order to
     * create the sourceandconverter object instead of the default one. If another
     * entity enters in conflict with this one, one the first one is called, and a warning
     * message should appear TODO
     * @param spimData object being opened
     * @param viewSetup viewsetup - helps to identify attributes and setup id
     * @return the SourceAndConverter newly created
     */
    default SourceAndConverter<?> makeSourceAndConverter(AbstractSpimData spimData, BasicViewSetup viewSetup) {
        throw new UnsupportedOperationException("makeSourceAndConverter method needs to be overriden if used");
    };

}

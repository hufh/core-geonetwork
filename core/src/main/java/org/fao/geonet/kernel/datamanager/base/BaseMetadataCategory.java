package org.fao.geonet.kernel.datamanager.base;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataCategory;
import org.fao.geonet.kernel.SvnManager;
import org.fao.geonet.kernel.datamanager.IMetadataCategory;
import org.fao.geonet.repository.MetadataCategoryRepository;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.repository.Updater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import jeeves.server.context.ServiceContext;

public class BaseMetadataCategory implements IMetadataCategory {

    @Autowired
    private MetadataRepository metadataRepository;
    @Autowired(required=false)
    private SvnManager svnManager;
    @Autowired
    private ApplicationContext _applicationContext;

    public void init(ServiceContext context, Boolean force) throws Exception {
//        this.metadataRepository = context.getBean(MetadataRepository.class);
//        this.svnManager = context.getBean(SvnManager.class);
    }

    /**
     * Adds a category to a metadata. Metadata is not reindexed.
     */
    @Override
    public void setCategory(ServiceContext context, String mdId, String categId) throws Exception {
        final MetadataCategoryRepository categoryRepository = getApplicationContext().getBean(MetadataCategoryRepository.class);

        final MetadataCategory newCategory = categoryRepository.findOne(Integer.valueOf(categId));
        final boolean[] changed = new boolean[1];
        getMetadataRepository().update(Integer.valueOf(mdId), new Updater<Metadata>() {
            @Override
            public void apply(@Nonnull Metadata entity) {
                changed[0] = !entity.getMetadataCategories().contains(newCategory);
                entity.getMetadataCategories().add(newCategory);
            }
        });

        if (changed[0]) {
            if (getSvnManager() != null) {
                getSvnManager().setHistory(mdId, context);
            }
        }
    }

    /**
     *
     * @param mdId
     * @param categId
     * @return
     * @throws Exception
     */
    @Override
    public boolean isCategorySet(final String mdId, final int categId) throws Exception {
        Set<MetadataCategory> categories = getMetadataRepository().findOne(mdId).getMetadataCategories();
        for (MetadataCategory category : categories) {
            if (category.getId() == categId) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param mdId
     * @param categId
     * @throws Exception
     */
    @Override
    public void unsetCategory(final ServiceContext context, final String mdId, final int categId) throws Exception {
        Metadata metadata = getMetadataRepository().findOne(mdId);

        if (metadata == null) {
            return;
        }
        boolean changed = false;
        for (MetadataCategory category : metadata.getMetadataCategories()) {
            if (category.getId() == categId) {
                changed = true;
                metadata.getMetadataCategories().remove(category);
                break;
            }
        }

        if (changed) {
            getMetadataRepository().save(metadata);
            if (getSvnManager() != null) {
                getSvnManager().setHistory(mdId + "", context);
            }
        }
    }

    /**
     *
     * @param mdId
     * @return
     * @throws Exception
     */
    @Override
    public Collection<MetadataCategory> getCategories(final String mdId) throws Exception {
        Metadata metadata = getMetadataRepository().findOne(mdId);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found with id: " + mdId);
        }

        return metadata.getMetadataCategories();
    }

    private SvnManager getSvnManager() {
        return svnManager;

    }

    private MetadataRepository getMetadataRepository() {
        return metadataRepository;

    }

    private ApplicationContext getApplicationContext() {
        final ConfigurableApplicationContext applicationContext = ApplicationContextHolder.get();
        return applicationContext == null ? _applicationContext : applicationContext;
    }
}
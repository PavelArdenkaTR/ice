package org.jbei.ice.web.panels;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.jbei.ice.lib.managers.ManagerException;
import org.jbei.ice.lib.managers.SampleManager;
import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.models.Location;
import org.jbei.ice.web.pages.EntryViewPage;

public class LocationItemViewPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private Integer index = null;
    private Location location = null;

    @SuppressWarnings("unchecked")
    public LocationItemViewPanel(String id, Integer counter, Location location) {
        super(id);

        this.setLocation(location);
        this.setIndex(counter);

        add(new Label("counter", counter.toString()));
        add(new Label("location", location.getLocation()));
        add(new Label("barcode", location.getBarcode()));
        add(new Label("wells", location.getWells()));
        add(new Label("notes", location.getNotes()));

        add(new Label("nColumns", "" + location.getnColumns()));
        add(new Label("nRows", "" + location.getnRows()));

        class DeleteLocationLink extends AjaxFallbackLink {

            private static final long serialVersionUID = 1L;

            public DeleteLocationLink(String id) {
                super(id);
                this.add(new SimpleAttributeModifier("onclick",
                        "return confirm('Delete this location?');"));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                LocationItemViewPanel thisPanel = (LocationItemViewPanel) getParent();
                Location location = thisPanel.getLocation();
                Entry entry = location.getSample().getEntry();

                location.getSample().getLocations().remove(location);
                try {
                    SampleManager.save(location.getSample());
                } catch (ManagerException e) {
                    e.printStackTrace();
                }

                setRedirect(true);
                setResponsePage(EntryViewPage.class, new PageParameters("0=" + entry.getId()
                        + ",1=samples"));
            }
        }

        class EditLocationLink extends AjaxFallbackLink {
            private static final long serialVersionUID = 1L;

            public EditLocationLink(String id) {
                super(id);
            }

            public void onClick(AjaxRequestTarget target) {
                boolean edit = true;
                LocationItemViewPanel thisPanel = (LocationItemViewPanel) getParent();

                LocationViewPanel locationViewPanel = (LocationViewPanel) thisPanel.getParent()
                        .getParent().getParent();
                for (Panel panel : locationViewPanel.getPanels()) {
                    if (panel instanceof LocationItemEditPanel) {
                        edit = false;
                    }
                }
                if (edit) {
                    Location location = thisPanel.getLocation();
                    int myIndex = locationViewPanel.getPanels().indexOf(thisPanel);
                    Panel newLocationEditPanel = new LocationItemEditPanel("locationItemPanel",
                            location);
                    locationViewPanel.getPanels().remove(myIndex);
                    locationViewPanel.getPanels().add(myIndex, newLocationEditPanel);
                    // I need to get the SampleViewPanel.
                    SampleViewPanel temp = (SampleViewPanel) thisPanel.getParent().getParent()
                            .getParent().getParent().getParent().getParent().getParent();
                    getPage().replace(temp);
                    target.addComponent(temp);
                }
            }
        }

        AjaxFallbackLink deleteLocationLink = new DeleteLocationLink("deleteLocationLink");
        deleteLocationLink.setOutputMarkupId(true);
        add(deleteLocationLink);

        AjaxFallbackLink editLocationLink = new EditLocationLink("editLocationLink");
        editLocationLink.setOutputMarkupId(true);
        add(editLocationLink);
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getIndex() {
        return index;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

}

package daris.client.ui.project;

import arc.gui.gwt.dnd.DragWidget;
import arc.gui.gwt.dnd.DropHandler;
import arc.gui.gwt.widget.label.Label;
import arc.gui.menu.Menu;
import arc.gui.object.SelectedObjectSet;
import arc.gui.object.display.ObjectDetailsDisplay;
import arc.gui.object.register.ObjectGUI;
import arc.gui.object.register.ObjectUpdateHandle;
import arc.gui.object.register.ObjectUpdateListener;
import arc.gui.window.Window;
import daris.client.model.project.Project;

public class ProjectRoleUserGUI implements ObjectGUI {

    public static final ProjectRoleUserGUI INSTANCE = new ProjectRoleUserGUI();

    private ProjectRoleUserGUI() {

    }

    @Override
    public String idToString(Object o) {

        return ((Project.RoleUser) o).toString();
    }

    @Override
    public String icon(Object o, int size) {

        return null;
    }

    @Override
    public Object reference(Object o) {
        return null;
    }

    @Override
    public boolean needToResolve(Object o) {
        return false;
    }

    @Override
    public void displayDetails(Object o, ObjectDetailsDisplay dd, boolean forEdit) {

    }

    @Override
    public void open(Window w, Object o) {
    }

    @Override
    public DropHandler dropHandler(Object o) {
        return null;
    }

    @Override
    public DragWidget dragWidget(Object o) {

        Project.RoleUser pru = (Project.RoleUser) o;
        return new DragWidget("role-user", new Label(pru.name()));
    }

    @Override
    public Menu actionMenu(Window w, Object o, SelectedObjectSet selected, boolean readOnly) {
        return null;
    }

    @Override
    public Menu memberActionMenu(Window w, Object o, SelectedObjectSet selected, boolean readOnly) {
        return null;
    }

    @Override
    public ObjectUpdateHandle createUpdateMonitor(Object o, ObjectUpdateListener ul) {
        return null;
    }
}

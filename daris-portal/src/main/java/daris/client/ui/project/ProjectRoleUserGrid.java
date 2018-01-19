package daris.client.ui.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;

import arc.gui.form.FormEditMode;
import arc.gui.gwt.data.DataLoadAction;
import arc.gui.gwt.data.DataLoadHandler;
import arc.gui.gwt.data.DataSource;
import arc.gui.gwt.data.filter.Filter;
import arc.gui.gwt.dnd.DropCheck;
import arc.gui.gwt.dnd.DropHandler;
import arc.gui.gwt.dnd.DropListener;
import arc.gui.gwt.widget.BaseWidget;
import arc.gui.gwt.widget.HTML;
import arc.gui.gwt.widget.list.ListGrid;
import arc.gui.gwt.widget.list.ListGridEntry;
import arc.gui.gwt.widget.list.ListGridRowContextMenuHandler;
import arc.gui.gwt.widget.list.ListGridRowDoubleClickHandler;
import arc.gui.gwt.widget.menu.ActionContextMenu;
import arc.gui.gwt.widget.scroll.ScrollPolicy;
import arc.gui.gwt.widget.tip.ToolTip;
import arc.gui.gwt.widget.tip.ToolTipHandler;
import arc.gui.menu.ActionEntry;
import arc.gui.menu.Menu;
import arc.mf.client.util.Action;
import arc.mf.client.util.IsValid;
import arc.mf.client.util.MustBeValid;
import arc.mf.client.util.ObjectUtil;
import arc.mf.client.util.StateChangeListener;
import arc.mf.client.util.Transformer;
import arc.mf.client.util.Validity;
import arc.mf.object.Null;
import arc.mf.object.ObjectMessageResponse;
import daris.client.model.project.DataUse;
import daris.client.model.project.Project;
import daris.client.model.project.ProjectRole;
import daris.client.model.project.messages.ProjectRoleUserList;
import daris.client.model.user.RoleUser;
import daris.client.ui.DObjectGUIRegistry;

public class ProjectRoleUserGrid extends ListGrid<Project.RoleUser>
        implements DropHandler, MustBeValid, StateChangeListener {

    private static class ProjectRoleUserTransformer
            extends Transformer<Project.RoleUser, ListGridEntry<Project.RoleUser>> {

        public static final ProjectRoleUserTransformer INSTANCE = new ProjectRoleUserTransformer();

        private ProjectRoleUserTransformer() {

        }

        @Override
        protected ListGridEntry<Project.RoleUser> doTransform(Project.RoleUser pru) throws Throwable {

            ListGridEntry<Project.RoleUser> prme = new ListGridEntry<Project.RoleUser>(pru);
            prme.set("id", pru.id());
            prme.set("name", pru.name());
            prme.set("role", pru.role());
            prme.set("dataUse", pru.dataUse());
            return prme;
        }
    }

    private static class ProjectRoleUserListTransformer
            extends Transformer<List<Project.RoleUser>, List<ListGridEntry<Project.RoleUser>>> {
        public static final ProjectRoleUserListTransformer INSTANCE = new ProjectRoleUserListTransformer();

        private ProjectRoleUserListTransformer() {

        };

        @Override
        protected List<ListGridEntry<Project.RoleUser>> doTransform(List<Project.RoleUser> prus) throws Throwable {

            if (prus != null) {
                if (!prus.isEmpty()) {
                    List<ListGridEntry<Project.RoleUser>> entries = new ArrayList<ListGridEntry<Project.RoleUser>>(
                            prus.size());
                    for (Project.RoleUser pru : prus) {
                        entries.add(ProjectRoleUserTransformer.INSTANCE.transform(pru));
                    }
                    return entries;
                }
            }
            return null;
        }
    }

    private static class ProjectRoleUserDataSource implements DataSource<ListGridEntry<Project.RoleUser>> {

        private Project _project;
        private FormEditMode _mode;

        private ProjectRoleUserDataSource(Project project, FormEditMode mode) {

            _project = project;
            _mode = mode;
        }

        @Override
        public boolean isRemote() {

            if (_mode.equals(FormEditMode.CREATE)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean supportCursor() {

            return false;
        }

        @Override
        public void load(final Filter f, final long start, final long end,
                final DataLoadHandler<ListGridEntry<Project.RoleUser>> lh) {

            if (_mode.equals(FormEditMode.CREATE)) {
                if (_project.roleUsers() == null) {
                    lh.loaded(0, 0, 0, null, null);
                } else {
                    doLoad(f, start, end, _project.roleUsers(), lh);
                }
            } else {
                new ProjectRoleUserList(_project).send(new ObjectMessageResponse<List<Project.RoleUser>>() {

                    @Override
                    public void responded(List<Project.RoleUser> prus) {

                        if (prus != null) {
                            if (!prus.isEmpty()) {
                                doLoad(f, start, end, prus, lh);
                                return;
                            }
                        }
                        lh.loaded(0, 0, 0, null, null);
                    }
                });
            }
        }

        private void doLoad(Filter f, long start, long end, List<Project.RoleUser> prus,
                final DataLoadHandler<ListGridEntry<Project.RoleUser>> lh) {

            if (prus != null) {
                Collections.sort(prus);
            }
            List<ListGridEntry<Project.RoleUser>> entries = ProjectRoleUserListTransformer.INSTANCE.transform(prus);
            int total = prus.size();
            int start0 = (int) start;
            int end0 = (int) end;
            if (start0 > 0 || end0 < total) {
                if (start0 >= total) {
                    entries = null;
                } else {
                    if (end0 > total) {
                        end0 = total;
                    }
                    entries = entries.subList(start0, end0);
                }
            }
            lh.loaded(start0, end0, entries == null ? 0 : total, entries, DataLoadAction.REPLACE);
        }
    }

    private Project _o;

    private FormEditMode _mode;

    public ProjectRoleUserGrid(Project project, FormEditMode mode) {

        super(new ProjectRoleUserDataSource(project, mode), ScrollPolicy.AUTO);
        _o = project;
        _mode = mode;
        addColumnDefn("name", "Name").setWidth(120);
        addColumnDefn("role", "Role").setWidth(120);
        addColumnDefn("dataUse", "Data Use").setWidth(80);

        fitToParent();
        setShowHeader(true);
        setShowRowSeparators(true);
        setMultiSelect(false);
        setFontSize(10);
        setCellSpacing(0);
        setCellPadding(1);
        setEmptyMessage("");
        setLoadingMessage("");
        setCursorSize(Integer.MAX_VALUE);

        setRowToolTip(new ToolTip<Project.RoleUser>() {

            @Override
            public void generate(Project.RoleUser prm, ToolTipHandler th) {

                th.setTip(new HTML(prm.toHTML()));

            }
        });

        /*
         * 
         */
        setObjectRegistry(DObjectGUIRegistry.get());

        /*
         * make drop target, and enable row drag (to delete member)
         */
        if (!_mode.equals(FormEditMode.READ_ONLY)) {
            setRowDoubleClickHandler(new ListGridRowDoubleClickHandler<Project.RoleUser>() {

                @Override
                public void doubleClicked(final Project.RoleUser pru, DoubleClickEvent event) {

                    if (pru == null) {
                        return;
                    }
                    int x = event.getClientX();
                    int y = event.getClientY();
                    ProjectMemberRoleSelector.showAt(x, y, new ProjectMemberRoleSelector.RoleSelectionListener() {

                        @Override
                        public void roleSelected(ProjectRole role, DataUse dataUse) {

                            if (role != null) {
                                pru.setRole(role);
                                pru.setDataUse(dataUse);
                                _o.addRoleUser(pru);
                                commitChangesAndRefresh();
                            }
                        }
                    });
                }
            });

            setRowContextMenuHandler(new ListGridRowContextMenuHandler<Project.RoleUser>() {

                @Override
                public void show(final Project.RoleUser pru, ContextMenuEvent event) {

                    final int x = event.getNativeEvent().getClientX();
                    final int y = event.getNativeEvent().getClientY();
                    Menu menu = new Menu("Role User");
                    menu.setShowTitle(true);
                    menu.add(new ActionEntry("Remove", new Action() {

                        @Override
                        public void execute() {

                            _o.removeRoleUser(pru);
                            commitChangesAndRefresh();
                        }
                    }));
                    menu.add(new ActionEntry("Set project role and data-use", new Action() {

                        @Override
                        public void execute() {

                            ProjectMemberRoleSelector.showAt(x, y,
                                    new ProjectMemberRoleSelector.RoleSelectionListener() {

                                        @Override
                                        public void roleSelected(ProjectRole role, DataUse dataUse) {

                                            if (role != null) {
                                                if (!role.equals(pru.role())
                                                        || !ObjectUtil.equals(dataUse, pru.dataUse())) {
                                                    pru.setRole(role);
                                                    pru.setDataUse(dataUse);
                                                    _o.addRoleUser(pru);
                                                    commitChangesAndRefresh();
                                                }
                                            }
                                        }
                                    });
                        }
                    }));
                    ActionContextMenu am = new ActionContextMenu(menu);
                    NativeEvent ne = event.getNativeEvent();
                    am.showAt(ne);
                }
            });
            enableDropTarget(false);
            setDropHandler(this);
            enableRowDrag();
        }
    }

    private void commitChangesAndRefresh() {

        commitChanges(new Action() {

            @Override
            public void execute() {

                ProjectRoleUserGrid.this.refresh();
            }
        });
    }

    private void commitChanges(final Action postAction) {

        if (_mode.equals(FormEditMode.UPDATE)) {
            _o.commitUsers(new ObjectMessageResponse<Null>() {

                @Override
                public void responded(Null r) {
                    postAction.execute();
                }
            });
        } else {
            if (postAction != null) {
                postAction.execute();
            }
        }
    }

    @Override
    public DropCheck checkCanDrop(Object o) {

        if (_mode.equals(FormEditMode.READ_ONLY)) {
            return DropCheck.CANNOT;
        }
        if (o instanceof RoleUser || o instanceof Project.RoleUser) {
            return DropCheck.CAN;
        }
        return DropCheck.CANNOT;
    }

    @Override
    public void drop(final BaseWidget target, final List<Object> objects, final DropListener dl) {

        if (objects == null) {
            dl.dropped(DropCheck.CANNOT);
            return;
        }
        if (objects.isEmpty()) {
            dl.dropped(DropCheck.CANNOT);
            return;
        }

        final ProjectMemberRoleSelector.RoleSelectionListener rsl = new ProjectMemberRoleSelector.RoleSelectionListener() {
            @Override
            public void roleSelected(ProjectRole role, DataUse dataUse) {

                if (role == null) {
                    dl.dropped(DropCheck.CANNOT);
                    return;
                }
                for (Object o : objects) {
                    if (o instanceof RoleUser) {
                        _o.addRoleUser(new Project.RoleUser(((RoleUser) o).name(), ((RoleUser) o).id(), role, dataUse));
                    } else if (o instanceof Project.RoleUser) {
                        Project.RoleUser pru = ((Project.RoleUser) o);
                        pru.setRole(role);
                        pru.setDataUse(dataUse);
                        _o.addRoleUser(pru);
                    }
                }
                // if (_mode == FormEditMode.UPDATE) {
                commitChangesAndRefresh();
                // } else if (_mode == FormEditMode.CREATE) {
                // notifyOfChangeInState();
                // }
                dl.dropped(DropCheck.CAN);
            }

        };

        ProjectMemberRoleSelector.showAt(target.absoluteLeft() + target.width() / 2,
                target.absoluteTop() + target.height() / 2, rsl);

    }

    private List<StateChangeListener> _cls;

    @Override
    public boolean changed() {

        if (_mode.equals(FormEditMode.READ_ONLY)) {
            return false;
        }
        if (_mode.equals(FormEditMode.CREATE) && _o.hasRoleUsers()) {
            return true;
        }
        return false;
    }

    @Override
    public void addChangeListener(StateChangeListener listener) {

        if (_cls == null) {
            _cls = new Vector<StateChangeListener>();
        }
        _cls.add(listener);
    }

    @Override
    public void removeChangeListener(StateChangeListener listener) {

        if (_cls != null) {
            _cls.remove(listener);
        }
    }

    @Override
    public Validity valid() {

        if (_mode.equals(FormEditMode.READ_ONLY)) {
            return IsValid.INSTANCE;
        }

        if (!_o.hasMembersOrRoleMembers()) {
            return new Validity() {

                @Override
                public boolean valid() {

                    return false;
                }

                @Override
                public String reasonForIssue() {

                    return "No member or role members are set.";
                }
            };
        }
        if (!_o.hasAdminUser()) {
            return new Validity() {

                @Override
                public boolean valid() {

                    return false;
                }

                @Override
                public String reasonForIssue() {

                    return "No project-administrator member or role member is set.";
                }
            };
        }
        return IsValid.INSTANCE;
    }

    @Override
    public void notifyOfChangeInState() {

        if (_cls == null) {
            return;
        }

        for (StateChangeListener cl : _cls) {
            cl.notifyOfChangeInState();
        }
    }

}
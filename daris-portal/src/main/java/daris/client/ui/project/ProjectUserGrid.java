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
import arc.mf.model.authentication.UserRef;
import arc.mf.object.Null;
import arc.mf.object.ObjectMessageResponse;
import daris.client.model.project.DataUse;
import daris.client.model.project.Project;
import daris.client.model.project.ProjectRole;
import daris.client.model.project.messages.ProjectUserList;
import daris.client.ui.DObjectGUIRegistry;

public class ProjectUserGrid extends ListGrid<Project.User> implements DropHandler, MustBeValid, StateChangeListener {

    private static class ProjectUserTransformer extends Transformer<Project.User, ListGridEntry<Project.User>> {

        public static final ProjectUserTransformer INSTANCE = new ProjectUserTransformer();

        private ProjectUserTransformer() {

        }

        @Override
        protected ListGridEntry<Project.User> doTransform(Project.User pu) throws Throwable {

            ListGridEntry<Project.User> pme = new ListGridEntry<Project.User>(pu);
            pme.set("authority", pu.user().domain().authority() == null ? null : pu.user().domain().authority().name());
            pme.set("domain", pu.user().domain());
            pme.set("user", pu.user().name());
            pme.set("role", pu.role());
            pme.set("dataUse", pu.dataUse());
            pme.set("id", pu.user().id());
            pme.set("name", pu.user().personName());
            pme.set("protocol",
                    pu.user().domain().authority() == null ? null : pu.user().domain().authority().protocol());
            pme.set("email", pu.user().email());
            pme.set("userString", pu.user().toString());
            return pme;
        }
    }

    private static class ProjectUserListTransformer
            extends Transformer<List<Project.User>, List<ListGridEntry<Project.User>>> {
        public static final ProjectUserListTransformer INSTANCE = new ProjectUserListTransformer();

        private ProjectUserListTransformer() {

        };

        @Override
        protected List<ListGridEntry<Project.User>> doTransform(List<Project.User> pus) throws Throwable {

            if (pus != null) {
                if (!pus.isEmpty()) {
                    List<ListGridEntry<Project.User>> entries = new ArrayList<ListGridEntry<Project.User>>(pus.size());
                    for (Project.User pu : pus) {
                        entries.add(ProjectUserTransformer.INSTANCE.transform(pu));
                    }
                    return entries;
                }
            }
            return null;
        }
    }

    private static class ProjectUserDataSource implements DataSource<ListGridEntry<Project.User>> {

        private Project _o;
        private FormEditMode _mode;

        private ProjectUserDataSource(Project o, FormEditMode mode) {

            _o = o;
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
                final DataLoadHandler<ListGridEntry<Project.User>> lh) {

            if (_mode.equals(FormEditMode.CREATE)) {
                if (_o.users() == null) {
                    lh.loaded(0, 0, 0, null, null);
                } else {
                    doLoad(f, start, end, _o.users(), lh);
                }
            } else {
                new ProjectUserList(_o).send(new ObjectMessageResponse<List<Project.User>>() {

                    @Override
                    public void responded(List<Project.User> pms) {

                        if (pms != null) {
                            if (!pms.isEmpty()) {
                                doLoad(f, start, end, pms, lh);
                                return;
                            }
                        }
                        lh.loaded(0, 0, 0, null, null);
                    }
                });
            }
        }

        private void doLoad(Filter f, long start, long end, List<Project.User> pms,
                final DataLoadHandler<ListGridEntry<Project.User>> lh) {

            if (pms != null) {
                Collections.sort(pms);
            }
            List<ListGridEntry<Project.User>> pmes = ProjectUserListTransformer.INSTANCE.transform(pms);
            int total = pms.size();
            int start0 = (int) start;
            int end0 = (int) end;
            if (start0 > 0 || end0 < total) {
                if (start0 >= total) {
                    pmes = null;
                } else {
                    if (end0 > total) {
                        end0 = total;
                    }
                    pmes = pmes.subList(start0, end0);
                }
            }
            lh.loaded(start0, end0, pmes == null ? 0 : total, pmes, DataLoadAction.REPLACE);
        }
    }

    private Project _o;

    private FormEditMode _mode;

    public ProjectUserGrid(Project o, FormEditMode mode) {

        super(new ProjectUserDataSource(o, mode), ScrollPolicy.AUTO);
        _o = o;
        _mode = mode;
        if (_mode.equals(FormEditMode.READ_ONLY)) {
            addColumnDefn("name", "Name").setWidth(100);
            addColumnDefn("authority", "Authority").setWidth(120);
            addColumnDefn("domain", "Domain").setWidth(100);
            addColumnDefn("user", "User").setWidth(100);
            addColumnDefn("role", "Role").setWidth(120);
            addColumnDefn("dataUse", "Data Use").setWidth(80);
        } else {
            addColumnDefn("role", "Role").setWidth(120);
            addColumnDefn("domain", "Domain").setWidth(100);
            addColumnDefn("user", "User").setWidth(100);
            addColumnDefn("dataUse", "Data Use").setWidth(80);
        }

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

        setRowToolTip(new ToolTip<Project.User>() {

            @Override
            public void generate(Project.User pu, ToolTipHandler th) {

                th.setTip(new HTML(pu.toHTML()));

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
            setRowDoubleClickHandler(new ListGridRowDoubleClickHandler<Project.User>() {

                @Override
                public void doubleClicked(final Project.User pu, DoubleClickEvent event) {

                    if (pu == null) {
                        return;
                    }
                    int x = event.getClientX();
                    int y = event.getClientY();
                    ProjectMemberRoleSelector.showAt(x, y, new ProjectMemberRoleSelector.RoleSelectionListener() {

                        @Override
                        public void roleSelected(ProjectRole role, DataUse dataUse) {

                            if (role != null) {
                                pu.setRole(role);
                                pu.setDataUse(dataUse);
                                _o.addUser(pu);
                                if (_mode == FormEditMode.UPDATE) {
                                    commitChangesAndRefresh();
                                } else if (_mode == FormEditMode.CREATE) {
                                    notifyOfChangeInState();
                                }
                            }
                        }
                    });
                }
            });

            setRowContextMenuHandler(new ListGridRowContextMenuHandler<Project.User>() {

                @Override
                public void show(final Project.User pu, ContextMenuEvent event) {

                    final int x = event.getNativeEvent().getClientX();
                    final int y = event.getNativeEvent().getClientY();
                    Menu menu = new Menu("User");
                    menu.setShowTitle(true);
                    menu.add(new ActionEntry("Remove", new Action() {

                        @Override
                        public void execute() {

                            _o.removeUser(pu);
                            commitChangesAndRefresh();
                        }
                    }));
                    menu.add(new ActionEntry("Set role and data-use", new Action() {

                        @Override
                        public void execute() {

                            ProjectMemberRoleSelector.showAt(x, y,
                                    new ProjectMemberRoleSelector.RoleSelectionListener() {

                                        @Override
                                        public void roleSelected(ProjectRole role, DataUse dataUse) {

                                            if (role != null) {
                                                if (!role.equals(pu.role())
                                                        || !ObjectUtil.equals(dataUse, pu.dataUse())) {
                                                    pu.setRole(role);
                                                    pu.setDataUse(dataUse);
                                                    _o.addUser(pu);
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

                ProjectUserGrid.this.refresh();
            }
        });
    }

    private void commitChanges(final Action postAction) {

        if (_mode.equals(FormEditMode.UPDATE)) {
            _o.commitUsers(new ObjectMessageResponse<Null>() {

                @Override
                public void responded(Null r) {
                    _changed = true;
                    ProjectUserGrid.this.notifyOfChangeInState();
                    postAction.execute();
                }
            });
        } else {
            notifyOfChangeInState();
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
        if (o instanceof UserRef || o instanceof Project.User) {
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
                    if (o instanceof UserRef) {
                        _o.addUser(new Project.User(((UserRef) o), role, dataUse));
                    } else if (o instanceof Project.User) {
                        Project.User pm = ((Project.User) o);
                        pm.setRole(role);
                        pm.setDataUse(dataUse);
                        _o.addUser(pm);
                    }
                }
                commitChangesAndRefresh();
                dl.dropped(DropCheck.CAN);
            }

        };

        ProjectMemberRoleSelector.showAt(target.absoluteLeft() + target.width() / 2,
                target.absoluteTop() + target.height() / 2, rsl);

    }

    private List<StateChangeListener> _cls;
    private boolean _changed = false;

    @Override
    public boolean changed() {

        if (_mode.equals(FormEditMode.READ_ONLY)) {
            return false;
        }
        return _changed;
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

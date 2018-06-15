
# ============================================================================
# role namespace: daris
# ============================================================================
authorization.role.namespace.create :namespace daris :ifexists ignore \
    :description "Namespace for daris framework roles"

# ============================================================================
# role: daris:basic-user
# ============================================================================
actor.grant :type role :name daris:basic-user \
    :perm < :access ACCESS :resource -type service system.session.self.describe > \
    :perm < :access ACCESS :resource -type service  system.session.output.get >

# ============================================================================
# role: pssd.model.doc.user
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.model.doc.user
actor.grant :type role :name daris:pssd.model.doc.user \
    :perm < :access ACCESS     :resource -type document:namespace daris > \
    :perm < :access PUBLISH    :resource -type document:namespace daris > \
    :perm < :access ADMINISTER :resource -type document:namespace daris > \
    :perm < :access ACCESS     :resource -type document:namespace : > \
    :perm < :access ACCESS     :resource -type document daris:pssd-object > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-object > \
    :perm < :access ACCESS     :resource -type document daris:pssd-filename > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-filename > \
    :perm < :access ACCESS     :resource -type document daris:pssd-project > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-project > \
    :perm < :access ACCESS     :resource -type document daris:pssd-subject > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-subject > \
    :perm < :access ACCESS     :resource -type document daris:pssd-ex-method > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-ex-method > \
    :perm < :access ACCESS     :resource -type document daris:pssd-dataset > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-dataset > \
    :perm < :access ACCESS     :resource -type document daris:pssd-transform > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-transform > \
    :perm < :access ACCESS     :resource -type document daris:pssd-acquisition > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-acquisition > \
    :perm < :access ACCESS     :resource -type document daris:pssd-derivation > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-derivation > \
    :perm < :access ACCESS     :resource -type document daris:pssd-method > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-method > \
    :perm < :access ACCESS     :resource -type document daris:pssd-method-subject > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-method-subject > \
    :perm < :access ACCESS     :resource -type document daris:pssd-method-rsubject > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-method-rsubject > \
    :perm < :access ACCESS     :resource -type document daris:pssd-notification > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-notification > \
    :perm < :access ACCESS     :resource -type document daris:pssd-project-harvest > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-project-harvest > \
    :perm < :access ACCESS     :resource -type document daris:pssd-project-owner > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-project-owner > \
    :perm < :access ACCESS     :resource -type document daris:pssd-project-governance > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-project-governance > \
    :perm < :access ACCESS     :resource -type document daris:pssd-project-research-category > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-project-research-category > \
    :perm < :access ACCESS     :resource -type document daris:pssd-publications > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-publications > \
    :perm < :access ACCESS     :resource -type document daris:pssd-related-services > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-related-services > \
    :perm < :access ACCESS     :resource -type document daris:pssd-role-member-registry > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-role-member-registry > \
    :perm < :access ACCESS     :resource -type document daris:pssd-dicom-server-registry > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-dicom-server-registry > \
    :perm < :access ACCESS     :resource -type document daris:pssd-shoppingcart-layout-pattern > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-shoppingcart-layout-pattern > \
    :perm < :access ACCESS     :resource -type document daris:pssd-dicom-ingest > \
    :perm < :access PUBLISH    :resource -type document daris:pssd-dicom-ingest > \
    :perm < :access ACCESS     :resource -type document daris:dicom-dataset > \
    :perm < :access PUBLISH    :resource -type document daris:dicom-dataset >

# reovke excessively permissive access to all document namespaces
actor.grant :type role :name daris:pssd.model.doc.user \
    :perm < :access ACCESS :resource -type document:namespace * >

# ============================================================================
# role: pssd.model.user
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.model.user

#    NOTE:
#    pssd.model.user: ( basic-user, federation-user, pssd.model.doc.user )
#    A DaRIS user holds pssd.model.user and {domain}.pssd.model.user roles.
actor.grant :type role :name daris:pssd.model.user \
    :role -type role daris:basic-user \
    :role -type role daris:pssd.model.doc.user \
    :role -type role daris:federation-user

# access to services
actor.grant :type role :name daris:pssd.model.user \
    :perm < :access ACCESS :resource -type service unimelb.* > \
    :perm < :access MODIFY :resource -type service unimelb.* > \
    :perm < :access ACCESS :resource -type service daris.* > \
    :perm < :access MODIFY :resource -type service daris.* > \
    :perm < :access ACCESS :resource -type service om.pssd.* > \
    :perm < :access MODIFY :resource -type service om.pssd.* > \
    :perm < :access ACCESS :resource -type service nig.* > \
    :perm < :access MODIFY :resource -type service nig.* > \
    :perm < :access ACCESS :resource -type service actor.have > \
    :perm < :access ADMINISTER :resource -type service actor.revoke > \
    :perm < :access ACCESS :resource -type service actor.self.* > \
    :perm < :access MODIFY :resource -type service actor.self.* > \
    :perm < :access ACCESS :resource -type service application.property.* > \
    :perm < :access ACCESS :resource -type service asset.acl.have > \
    :perm < :access ACCESS :resource -type service asset.archive.content.* > \
    :perm < :access ACCESS :resource -type service asset.content.get > \
    :perm < :access MODIFY :resource -type service asset.create > \
    :perm < :access MODIFY :resource -type service asset.destroy > \
    :perm < :access ACCESS :resource -type service asset.doc.namespace.list > \
    :perm < :access ACCESS :resource -type service asset.doc.type.describe > \
    :perm < :access ACCESS :resource -type service asset.doc.type.exists > \
    :perm < :access ACCESS :resource -type service asset.doc.type.list > \
    :perm < :access ACCESS :resource -type service asset.doc.template.as.xml > \
    :perm < :access ACCESS :resource -type service asset.meta.transform.profile.describe > \
    :perm < :access ACCESS :resource -type service asset.model.* > \
    :perm < :access ACCESS :resource -type service asset.namespace.get > \
    :perm < :access ACCESS :resource -type service asset.namespace.describe > \
    :perm < :access ACCESS :resource -type service asset.path.generate > \
    :perm < :access MODIFY :resource -type service asset.set > \
    :perm < :access MODIFY :resource -type service asset.soft.destroy > \
    :perm < :access ACCESS :resource -type service asset.transcode.describe > \
    :perm < :access ACCESS :resource -type service authentication.domain.* > \
    :perm < :access ACCESS :resource -type service authentication.user.* > \
    :perm < :access MODIFY :resource -type service authorization.role.create > \
    :perm < :access MODIFY :resource -type service citeable.id.create > \
    :perm < :access ACCESS :resource -type service citeable.named.id.describe > \
    :perm < :access ACCESS :resource -type service citeable.name.list > \
    :perm < :access ACCESS :resource -type service dictionary.* > \
    :perm < :access MODIFY :resource -type service dictionary.* > \
    :perm < :access ACCESS :resource -type service package.describe > \
    :perm < :access ACCESS :resource -type service package.list > \
    :perm < :access ACCESS :resource -type service package.exists > \
    :perm < :access ACCESS :resource -type service secure.identity.token.* > \
    :perm < :access MODIFY :resource -type service secure.identity.token.* > \
    :perm < :access ACCESS :resource -type service secure.shell.* > \
    :perm < :access MODIFY :resource -type service secure.shell.* > \
    :perm < :access ACCESS :resource -type service secure.wallet.* > \
    :perm < :access MODIFY :resource -type service secure.wallet.* > \
    :perm < :access ACCESS :resource -type service server.database.describe > \
    :perm < :access MODIFY :resource -type service server.io.job.create > \
    :perm < :access MODIFY :resource -type service server.io.write > \
    :perm < :access MODIFY :resource -type service server.io.write.finish > \
    :perm < :access MODIFY :resource -type service server.log > \
    :perm < :access ACCESS :resource -type service server.ping > \
    :perm < :access MODIFY :resource -type service server.task.named.begin > \
    :perm < :access MODIFY :resource -type service server.task.named.end > \
    :perm < :access ACCESS :resource -type service server.version > \
    :perm < :access MODIFY :resource -type service service.background.abort > \
    :perm < :access ACCESS :resource -type service service.background.describe > \
    :perm < :access ACCESS :resource -type service shopping.cart.* > \
    :perm < :access MODIFY :resource -type service shopping.cart.* > \
    :perm < :access ACCESS :resource -type service sink.* > \
    :perm < :access ACCESS :resource -type service system.events.* > \
    :perm < :access ACCESS :resource -type service system.logon > \
    :perm < :access ACCESS :resource -type service system.logoff > \
    :perm < :access ACCESS :resource -type service type.list > \
    :perm < :access ACCESS :resource -type service type.describe > \
    :perm < :access ACCESS :resource -type service type.ext.types > \
    :perm < :access ACCESS :resource -type service user.self.* > \
    :perm < :access MODIFY :resource -type service user.self.* > \
    :perm < :access ACCESS :resource -type service user.exists > \
    :perm < :access ACCESS :resource -type service user.describe > 

# access to role namespaces (required since server version 4.7.018)
if { [lsearch -exact [xvalues type/access/@name [authorization.resource.type.describe :type role:namespace]] ACCESS] != -1 } {
    actor.grant :type role :name daris:pssd.model.user \
        :perm < :access ACCESS :resource -type role:namespace * >
}


# access to dictionary namespace: daris
actor.grant :type role :name daris:pssd.model.user \
    :perm < :access ACCESS :resource -type dictionary:namespace daris >

# access to dictionary namespace: daris-tags
actor.grant :type role :name daris:pssd.model.user \
    :perm < :access ADMINISTER :resource -type dictionary:namespace daris-tags >

# ============================================================================
# role: pssd.model.power.user
# ============================================================================
# This role inherits from pssd.model.user. It has some extra rights to explore
# more of the system from aterm. Should be granted directly to a user
authorization.role.create :ifexists ignore :role daris:pssd.model.power.user

actor.grant :type role :name daris:pssd.model.power.user \
    :role -type role daris:pssd.model.user \
    :perm < :access MODIFY :resource -type service dictionary.add > \
    :perm < :access MODIFY :resource -type service dictionary.destroy > \
    :perm < :access MODIFY :resource -type service dictionary.entry.add > \
    :perm < :access MODIFY :resource -type service dictionary.entry.remove > \
    :perm < :access MODIFY :resource -type service asset.doc.type.create > \
    :perm < :access MODIFY :resource -type service asset.doc.type.destroy  > \
    :perm < :access ADMINISTER :resource -type service server.log.display >


# ============================================================================
# role: pssd.project.create
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.project.create

actor.grant :type role :name daris:pssd.project.create \
    :perm < :access ACCESS :resource -type service authentication.user.exists > \
    :perm < :access MODIFY :resource -type service citeable.named.id.create > \
    :perm < :access ADMINISTER :resource -type service user.authority.grant >

# ============================================================================
# role: pssd.subject.create
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.subject.create

actor.grant :type role :name daris:pssd.subject.create \
    :perm < :access MODIFY :resource -type service citeable.named.id.create >

# ============================================================================
# role: pssd.r-subject.admin
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.r-subject.admin

actor.grant :type role :name daris:pssd.r-subject.admin \
    :perm < :access MODIFY :resource -type service citeable.named.id.create >

# ============================================================================
# role: pssd.r-subject.guest
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.r-subject.guest

# ============================================================================
# Role: pssd.object.guest
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.object.guest

# ============================================================================
# Role: pssd.object.admin
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.object.admin

# grant pssd.object.admin role to system-administrator role so that 
# the system administrators can access the meta data).
actor.grant :type role :name system-administrator \
    :role -type role daris:pssd.object.admin

# ============================================================================
# role: pssd.administrator 
# ============================================================================
# Holders of this role should be able to undertake DaRIS admin activities
# without the full power of system:administrator.  Admin services
# require permission ADMINISTER to operate. Also grants the essentials
# package administrator role.
authorization.role.create :ifexists ignore :role daris:pssd.administrator

actor.grant :type role :name daris:pssd.administrator \
    :role -type role daris:pssd.object.admin \
    :role -type role daris:pssd.model.power.user \
    :role -type role daris:pssd.project.create \
    :role -type role daris:essentials.administrator \
    :role -type role daris:pssd.subject.create \
    :perm < :access ADMINISTER :resource -type dictionary:namespace daris > \
    :perm < :access ADMINISTER :resource -type dictionary:namespace daris-tags > \
    :perm < :access ADMINISTER :resource -type document:namespace daris > \
    :perm < :access ADMINISTER :resource -type role:namespace daris > \
    :perm < :access ADMINISTER :resource -type service om.pssd.* > \
    :perm < :access ADMINISTER :resource -type service daris.* >
     
# ============================================================================
# role: pssd.dicom-ingest
# ============================================================================
authorization.role.create :ifexists ignore :role daris:pssd.dicom-ingest

# NOTE: parallel approach to user roles.  We will define this structure
# dicom-user : {nig.pssd.dicom-ingest, pssd.dicom-ingest : dicom-ingest} 
# but could be user : {nig.pssd.dicom-ingest, pssd.dicom-ingest, dicom-ingest}
# dicom-ingest is created in the essentials package
actor.grant :type role :name daris:pssd.dicom-ingest \
    :role -type role dicom-ingest \
    :role -type role daris:pssd.model.doc.user \
    :role -type role daris:pssd.object.admin \
    :perm < :access ADMINISTER :resource -type role:namespace daris > \
    :perm < :access ACCESS     :resource -type dictionary:namespace daris > \
    :perm < :access ACCESS     :resource -type service actor.describe > \
    :perm < :access ACCESS     :resource -type service actor.have > \
    :perm < :access ACCESS     :resource -type service asset.get > \
    :perm < :access ACCESS     :resource -type service asset.doc.type.describe > \
    :perm < :access ACCESS     :resource -type service asset.doc.template.as.xml > \
    :perm < :access ACCESS     :resource -type service asset.query > \
    :perm < :access ACCESS     :resource -type service citeable.id.exists > \
    :perm < :access MODIFY     :resource -type service citeable.id.import > \
    :perm < :access MODIFY     :resource -type service citeable.named.id.create > \
    :perm < :access ACCESS     :resource -type service citeable.root.get > \
    :perm < :access ACCESS     :resource -type service dicom.metadata.get > \
    :perm < :access MODIFY     :resource -type service dicom.metadata.populate > \
    :perm < :access ACCESS     :resource -type service om.pssd.collection.member.list > \
    :perm < :access MODIFY     :resource -type service om.pssd.dataset.derivation.create  > \
    :perm < :access MODIFY     :resource -type service om.pssd.dataset.derivation.update > \
    :perm < :access MODIFY     :resource -type service om.pssd.dataset.primary.create  > \
    :perm < :access MODIFY     :resource -type service om.pssd.dataset.primary.update > \
    :perm < :access ACCESS     :resource -type service om.pssd.ex-method.step.study.find  > \
    :perm < :access ACCESS     :resource -type service om.pssd.ex-method.step.describe > \
    :perm < :access ACCESS     :resource -type service om.pssd.ex-method.study.step.find  > \
    :perm < :access ACCESS     :resource -type service om.pssd.object.exists > \
    :perm < :access ACCESS     :resource -type service om.pssd.object.describe > \
    :perm < :access ACCESS     :resource -type service om.pssd.object.type > \
    :perm < :access MODIFY     :resource -type service om.pssd.object.destroy > \
    :perm < :access ACCESS     :resource -type service om.pssd.project.mail.send > \
    :perm < :access MODIFY     :resource -type service om.pssd.project.members.list  > \
    :perm < :access MODIFY     :resource -type service om.pssd.role-member-registry.list > \
    :perm < :access MODIFY     :resource -type service om.pssd.subject.create > \
    :perm < :access MODIFY     :resource -type service om.pssd.subject.clone > \
    :perm < :access MODIFY     :resource -type service om.pssd.subject.update  > \
    :perm < :access ACCESS     :resource -type service om.pssd.subject.method.find  > \
    :perm < :access MODIFY     :resource -type service om.pssd.study.create > \
    :perm < :access MODIFY     :resource -type service om.pssd.study.update > \
    :perm < :access ACCESS     :resource -type service server.identity > \
    :perm < :access MODIFY     :resource -type service server.log > \
    :perm < :access ACCESS     :resource -type service system.session.self.describe  > \
    :perm < :access ACCESS     :resource -type service user.describe > \
    :perm < :access ACCESS     :resource -type service user.self.describe >

if { [lsearch -exact [xvalues type/access/@name [authorization.resource.type.describe :type role:namespace]] ACCESS] != -1 } {
    actor.grant :type role :name daris:pssd.dicom-ingest \
        :perm < :access ACCESS :resource -type role:namespace * >
}

# ============================================================================
# detect if "Transform framework" is installed. If it is, set the roles...
# ============================================================================
source role-permissions-transform.tcl

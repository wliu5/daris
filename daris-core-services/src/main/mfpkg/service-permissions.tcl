# ============================================================================
# special services require system-administrator role (to be improved)
# ============================================================================
actor.grant :type plugin:service :name daris.project.user.add           :role -type role system-administrator
actor.grant :type plugin:service :name daris.project.user.list          :role -type role system-administrator 
actor.grant :type plugin:service :name daris.project.user.set           :role -type role system-administrator
actor.grant :type plugin:service :name daris.project.user.remove        :role -type role system-administrator 

actor.grant :type plugin:service :name om.pssd.project.create           :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.project.members.add      :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.project.members.remove   :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.project.members.replace  :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.project.update           :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.user.revoke              :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.user.role.grant          :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.user.role.revoke         :role -type role system-administrator

# r-subject services (deprecated)
actor.grant :type plugin:service :name om.pssd.r-subject.admin.add      :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.r-subject.admin.remove   :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.r-subject.create         :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.r-subject.guest.add      :role -type role system-administrator
actor.grant :type plugin:service :name om.pssd.r-subject.guest.remove   :role -type role system-administrator

# ============================================================================
# services granted with service-user role
# ============================================================================
actor.grant :type plugin:service :name daris.archive.content.list             :role -type role service-user
actor.grant :type plugin:service :name daris.archive.content.get              :role -type role service-user
actor.grant :type plugin:service :name daris.archive.content.image.get        :role -type role service-user
actor.grant :type plugin:service :name daris.collection.dicom.dataset.count   :role -type role service-user
actor.grant :type plugin:service :name daris.downloader.get                   :role -type role service-user  :perm < :access ADMINISTER :resource -type service server.java.environment >
actor.grant :type plugin:service :name daris.dicom.onsend.user.list           :role -type role service-user
actor.grant :type plugin:service :name daris.dicom.send                       :role -type role service-user
actor.grant :type plugin:service :name daris.dicom.local.ae.title.list        :role -type role service-user
actor.grant :type plugin:service :name daris.object.attach                    :role -type role service-user
actor.grant :type plugin:service :name daris.object.attachment.get            :role -type role service-user
actor.grant :type plugin:service :name daris.object.attachment.list           :role -type role service-user
actor.grant :type plugin:service :name daris.object.children.list             :role -type role service-user
actor.grant :type plugin:service :name daris.object.children.count            :role -type role service-user
actor.grant :type plugin:service :name daris.object.child.cursor.get          :role -type role service-user
actor.grant :type plugin:service :name daris.object.detach                    :role -type role service-user
actor.grant :type plugin:service :name daris.object.path.find                 :role -type role service-user
actor.grant :type plugin:service :name daris.object.tag.add                   :role -type role service-user
actor.grant :type plugin:service :name daris.object.tag.remove                :role -type role service-user
actor.grant :type plugin:service :name daris.path.expression.add              :role -type role service-user
actor.grant :type plugin:service :name daris.path.expression.list             :role -type role service-user
actor.grant :type plugin:service :name daris.path.expression.remove           :role -type role service-user
actor.grant :type plugin:service :name daris.project.dictionary.create                   :role -type role service-user
actor.grant :type plugin:service :name daris.project.dictionary.destroy                  :role -type role service-user
actor.grant :type plugin:service :name daris.project.dictionary.list                     :role -type role service-user
actor.grant :type plugin:service :name daris.project.dictionary.namespace.create         :role -type role service-user
actor.grant :type plugin:service :name daris.project.dictionary.namespace.describe       :role -type role service-user
actor.grant :type plugin:service :name daris.repository.description.set       :role -type role service-user 
actor.grant :type plugin:service :name daris.repository.description.get       :role -type role service-user 
actor.grant :type plugin:service :name daris.repository.describe              :role -type role service-user
actor.grant :type plugin:service :name daris.role-user.describe               :role -type role service-user
actor.grant :type plugin:service :name daris.role-user.list                   :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.content.add         :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.content.clear       :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.content.list        :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.content.remove      :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.create              :role -type role service-user :perm < :access ADMINISTER :resource -type service application.property.* >
actor.grant :type plugin:service :name daris.shoppingcart.describe            :role -type role service-user :perm < :access ADMINISTER :resource -type service application.property.* >
actor.grant :type plugin:service :name daris.shoppingcart.destination.list    :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.destroy             :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.exists              :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.order               :role -type role service-user :perm < :access ADMINISTER :resource -type service application.property.* >
actor.grant :type plugin:service :name daris.shoppingcart.template.create     :role -type role service-user
actor.grant :type plugin:service :name daris.shoppingcart.template.destroy    :role -type role service-user
actor.grant :type plugin:service :name daris.user.describe                    :role -type role service-user
actor.grant :type plugin:service :name daris.user.list                        :role -type role service-user
actor.grant :type plugin:service :name om.pssd.announcement.create            :role -type role service-user
actor.grant :type plugin:service :name om.pssd.announcement.describe          :role -type role service-user
actor.grant :type plugin:service :name om.pssd.announcement.destroy           :role -type role service-user
actor.grant :type plugin:service :name om.pssd.announcement.list              :role -type role service-user
actor.grant :type plugin:service :name om.pssd.collection.members             :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.derivation.create      :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.derivation.find        :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.derivation.update      :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.primary.create         :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.primary.update         :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.processed.count        :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.processed.destroy      :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.processed.destroyable.count       :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.processed.destroyable.exists      :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.processed.exists       :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.processed.input.list   :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dataset.unprocessed.list       :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dicom.ae.list                  :role -type role service-user
actor.grant :type plugin:service :name om.pssd.dicom.anonymize                :role -type role service-user
actor.grant :type plugin:service :name om.pssd.ex-method.step.transform.find  :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.describe                :role -type role service-user :role -type role daris:pssd.r-subject.guest
actor.grant :type plugin:service :name om.pssd.object.destroy                 :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.exists                  :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.find                    :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.icon.get                :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.lock                    :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.session.lock            :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.session.unlock          :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.add                 :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.describe            :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.create   :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.destroy  :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.entry.add           :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.entry.list          :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.entry.remove        :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.get                 :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.global.create       :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.global.destroy      :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.global.entry.add    :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.global.entry.list   :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.dictionary.global.entry.remove :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.exists              :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.list                :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.remove              :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.tag.remove.all          :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.thumbnail.get           :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.thumbnail.image.get     :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.thumbnail.set           :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.thumbnail.unset         :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.type                    :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.unlock                  :role -type role service-user
actor.grant :type plugin:service :name om.pssd.object.update                  :role -type role service-user
actor.grant :type plugin:service :name om.pssd.project.mail.send              :role -type role service-user
actor.grant :type plugin:service :name om.pssd.project.members.list           :role -type role service-user
actor.grant :type plugin:service :name om.pssd.study.create                   :role -type role service-user
actor.grant :type plugin:service :name om.pssd.study.update                   :role -type role service-user
actor.grant :type plugin:service :name om.pssd.subject.create                 :role -type role service-user
actor.grant :type plugin:service :name om.pssd.subject.update                 :role -type role service-user
actor.grant :type plugin:service :name om.pssd.transform.find                 :role -type role service-user
actor.grant :type plugin:service :name om.pssd.type.metadata.list             :role -type role service-user
actor.grant :type plugin:service :name om.pssd.user.can.access                :role -type role service-user
actor.grant :type plugin:service :name om.pssd.user.can.create                :role -type role service-user
actor.grant :type plugin:service :name om.pssd.user.can.destroy               :role -type role service-user
actor.grant :type plugin:service :name om.pssd.user.can.modify                :role -type role service-user



































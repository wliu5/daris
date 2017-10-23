package nig.mf.dicom.plugin.util;

import java.util.Collection;
import java.util.Date;

import nig.mf.pssd.CiteableIdUtil;
import nig.util.DateUtil;
import arc.mf.plugin.dicom.DicomPersonName;
import arc.xml.XmlDoc;

/**
 * Class to hold a few patient related details from the mf-dicom-patient
 * document
 * 
 * Leading and trailing spaces are pulled off strings (e.g. name fields) before
 * being stored here
 * 
 * @author nebk
 *
 */
public class DICOMPatient {

    private String _firstName;
    private String _lastName;
    private String _fullName;
    private String _sex;
    private Date _dob;
    private String _id;

    public DICOMPatient(XmlDoc.Element r) throws Throwable {
        if (r != null)
            create(r);
    }

    public String getFirstName() {
        return _firstName;
    };

    public String getLastName() {
        return _lastName;
    };

    public String getFullName() {
        return _fullName;
    };

    public String getSex() {
        return _sex;
    };

    public Date getDOB() {
        return _dob;
    };

    public String getID() {
        return _id;
    };

    /**
     * Constructs last^first as best it can
     * 
     * @return
     */
    public String nameForDICOMFile() {
        if (_firstName != null && _lastName != null) {
            return _lastName + "^" + _firstName;
        } else if (_lastName != null) {
            return _lastName;
        } else if (_firstName != null) {
            return "^" + _firstName;
        } else {
            return null;
        }
    }

    public String toString() {
        String t = "Name = " + _firstName + " " + _lastName + "\n" + "Sex  = "
                + _sex + "\n" + "DOB  = " + _dob + "\n" + "ID   = " + _id;
        return t;
    }

    public Boolean hasBothNames() {
        return (_firstName != null && _lastName != null);
    }

    /**
     * Need at least _lastName to match First name is optional (can be null in
     * both)
     * 
     * @param findSubectMethod
     *            'id', 'name', 'name+' (name + sex + dob), or 'all' (id, name,
     *            dob, sex)
     * @param oldPatientMeta
     * @param new_firstName
     * @param newLastName
     * @param newSex
     * @param newDateOfBirth
     * @param newID
     * @return
     * @throws Throwable
     */
    public static boolean matchDICOMDetail(String findSubjectMethod,
            XmlDoc.Element oldPatientMeta, DicomPersonName newName,
            String newSex, Date newDOB, String newID) throws Throwable {
        if (oldPatientMeta == null)
            return false;

        if (findSubjectMethod.equalsIgnoreCase("id")) {
            return stringsMatch(oldPatientMeta.value("id"), newID, true);
        } else if (findSubjectMethod.equalsIgnoreCase("name")) {
            return dicomNamesMatch(newName, oldPatientMeta);
        } else if (findSubjectMethod.equalsIgnoreCase("name+")) {
            if (!dicomNamesMatch(newName, oldPatientMeta))
                return false;
            if (!stringsMatch(oldPatientMeta.value("sex"), newSex, true))
                return false;
            if (!dicomDOBMatch(oldPatientMeta.dateValue("dob"), newDOB))
                return false;
            return true;
        } else if (findSubjectMethod.equalsIgnoreCase("all")) {
            if (!stringsMatch(oldPatientMeta.value("id"), newID, true))
                return false;
            if (!dicomNamesMatch(newName, oldPatientMeta))
                return false;
            if (!stringsMatch(oldPatientMeta.value("sex"), newSex, true))
                return false;
            if (!dicomDOBMatch(oldPatientMeta.dateValue("dob"), newDOB))
                return false;
            return true;
        }
        return false;
    }

    public static Boolean dicomDOBMatch(Date oldDOB, Date newDOB)
            throws Throwable {
        if (oldDOB == null && newDOB == null) {
            // Both null is considered a match
            return true;
        } else {
            if (oldDOB != null && newDOB != null) {
                return DateUtil.areDatesOfBirthEqual(oldDOB, newDOB);
            }
        }
        return false;
    }

    public static boolean stringsMatch(String oldStr, String newStr,
            boolean ignoreCase) {
        if (oldStr == null && newStr == null) {
            // If they are both null, then we can't say whether the
            // value is the same or not and we consider this a match.
            return true;
        } else {
            if (oldStr != null && newStr != null) {
                if (ignoreCase) {
                    if (oldStr.equalsIgnoreCase(newStr)) {
                        return true;
                    }
                } else {
                    if (oldStr.equals(newStr)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Boolean dicomNamesMatch(DicomPersonName newName,
            XmlDoc.Element oldPatientMeta) throws Throwable {
        // COnstruct fullnames and compare.
        // If there are CIDs in the name, treat them as null as we don't
        // save/compare CIDs.
        String newFullName = null;
        if (newName != null) {
            newFullName = makeFullName(newName.first(), newName.last());
            if (CiteableIdUtil.isCiteableId(newFullName))
                newFullName = null;
        }

        //
        String oldFullName = null;
        if (oldPatientMeta != null) {
            String oldLastName = oldPatientMeta.value("name[@type='last']");
            String old_firstName = oldPatientMeta.value("name[@type='first']");
            oldFullName = makeFullName(old_firstName, oldLastName);
            if (CiteableIdUtil.isCiteableId(oldFullName))
                oldFullName = null;

        }

        //
        return stringsMatch(newFullName, oldFullName, true);
    }

    private static String makeFullName(String first, String last) {
        String full = null;
        if (first != null && last != null) {
            full = first + " " + last;
        } else if (last != null) {
            full = last;
        } else if (first != null) {
            full = first;
        }
        return full;
    }

    private void create(XmlDoc.Element r) throws Throwable {
        Collection<XmlDoc.Element> names = r.elements("name");
        if (names != null && names.size() > 0) {
            for (XmlDoc.Element name : names) {
                String type = name.value("@type");
                if (type.equals("first")) {
                    _firstName = name.value().trim();
                } else if (type.equals("last")) {
                    _lastName = name.value().trim();
                } else if (type.equals("full")) {
                    _fullName = name.value().trim();
                }
            }

			 // Construct a full name if not native
			 if (_fullName == null) {
				 if (_firstName != null)
					 _fullName = _firstName;
				 if (_lastName != null) {
					 if (_fullName == null) {
						 _fullName = _lastName;
					 } else {
						 _fullName += " " + _lastName;
					 }
				 }
			 } else {

				 // If it's  missing Construct a first/last name if possible from  fullname
				 if (_firstName==null) {
					 String[] t = _fullName.split(" ");
					 if (t.length==2) {
						 _firstName = t[0];
					 }
				 }
				 if (_lastName==null) {
					 String[] t = _fullName.split(" ");
					 if (t.length==2) {
						 _lastName = t[1];
					 }
				 }
			 }
			 

            //
            _dob = r.dateValue("dob");
            _sex = r.value("sex");
            if (_sex != null) {
            	_sex = _sex.trim();
            }
            _id = r.value("id");
        }
    }

}
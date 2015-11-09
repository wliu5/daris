package daris.client.model.archive.messages;

import java.util.List;

import arc.mf.client.Output;
import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.object.ObjectMessage;
import daris.client.model.archive.ArchiveEntry;
import daris.client.model.archive.ArchiveEntryCollectionRef;
import daris.client.util.DownloadUtil;

public class ArchiveContentGet extends ObjectMessage<ArchiveEntry> {

    private ArchiveEntryCollectionRef _arc;
    private int _idx;

    /**
     * 
     * @param arc
     * @param idx
     *            starts from one.
     */
    public ArchiveContentGet(ArchiveEntryCollectionRef arc, int idx) {
        _arc = arc;
        _idx = idx;
    }

    @Override
    protected void messageServiceArgs(XmlWriter w) {
        if (_arc.assetId() != null) {
            w.add("id", _arc.assetId());
        } else {
            w.add("cid", _arc.citeableId());
        }
        // _idx starts from 1.
        w.add("idx", _idx);
    }

    @Override
    protected String messageServiceName() {
        return "daris.archive.content.get";
    }

    @Override
    protected ArchiveEntry instantiate(XmlElement xe) throws Throwable {
        XmlElement ee = xe.element("entry");
        if (ee != null) {
            return new ArchiveEntry(ee);
        }
        return null;
    }

    @Override
    protected String objectTypeName() {
        return null;
    }

    @Override
    protected String idToString() {
        return String.valueOf(_idx);
    }

    @Override
    protected int numberOfOutputs() {
        return 1;
    }

    @Override
    protected void process(ArchiveEntry o, List<Output> outputs)
            throws Throwable {
        Output output = outputs.get(0);
        DownloadUtil.download(output, o.fileName());
    }
}

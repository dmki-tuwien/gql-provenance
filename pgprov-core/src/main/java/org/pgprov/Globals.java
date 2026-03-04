package org.pgprov;

public class Globals {

    // annotation keys
    public static final String NODE_ANNOT_KEY = "__n";
    public static final String EDGE_ANNOT_KEY = "__e";
    public static final String LBL_ANNOT_KEY_PREFIX = "__l_";
    public static final String PROP_ANNOT_KEY_PREFIX = "__k_";

    // annotation prefixes
    public static final String NODE_ANNOT_PREFIX = "annn_";
    public static final String EDGE_ANNOT_PREFIX = "anne_";
    public static final String PROP_ANNOT_PREFIX = "_k_";
    public static final String LBL_ANNOT_PREFIX = "_l_";

    // variable naming
    public static final String ANONYMOUS_VAR_PREFIX = "x";
    public static final String TEMP_VAR_LIST_PREFIX = "prov_temp_list_";
    public static final String VAR_PREFIX = "prov_";
    public static final String TEMP_VAR_PREFIX = "prov_temp_";
    public static final String TEMP_PATH_PREFIX = "provpath_temp_";
    public static final String PATH_PREFIX = "provpath_";
    public static final String EXTERNAL_VAR_VALUE = "provvar";

    public static String ID_FUNCTION = "elementId";

    public enum ProcessStage {
        SQL_TRANSLATION,            //Why-provenance + Where-provenance
        SQL_TRANSLATION_WHERE_PROVENANCE, // Where-provenance
        REWRITE_WHY_PROVENANCE,
        REWRITE_WHERE_PROVENANCE,
        INDIVIDUAL_QUERY_PROCESS
    }

    public enum ProvenanceType {
        WHY_PROV,
        WHERE_PROV,
        HOW_PROV
    }
}

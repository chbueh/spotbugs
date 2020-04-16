package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.npe.PointerUsageRequiringNonNullValue;
import edu.umd.cs.findbugs.ba.npe.ReturnPathType;
import edu.umd.cs.findbugs.ba.npe.ReturnPathTypeDataflow;
import edu.umd.cs.findbugs.ba.npe.UsagesRequiringNonNullValues;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.props.GeneralWarningProperty;
import edu.umd.cs.findbugs.props.WarningProperty;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import org.apache.bcel.classfile.Method;

public class FindDerefHelper {

    // Transient state
    private ClassContext classContext;

    private Method method;

    public FindDerefHelper(ClassContext classContext, Method method) {
        this.classContext = classContext;
        this.method = method;
    }

    void addPropertiesForMethodContainingWarning(WarningPropertySet<WarningProperty> propertySet) {
        XMethod xMethod = XFactory.createXMethod(classContext.getJavaClass(), method);

        boolean uncallable = !AnalysisContext.currentXFactory().isCalledDirectlyOrIndirectly(xMethod) && xMethod.isPrivate();

        if (uncallable) {
            propertySet.addProperty(GeneralWarningProperty.IN_UNCALLABLE_METHOD);
        }
    }

    boolean isDoomed(Location loc) {
        ReturnPathTypeDataflow rptDataflow;
        try {
            rptDataflow = classContext.getReturnPathTypeDataflow(method);

            ReturnPathType rpt = rptDataflow.getFactAtLocation(loc);

            return !rpt.canReturnNormally();
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error getting return path type", e);
            return false;
        }
    }

    String getDescription(Location loc, ValueNumber refValue, ValueNumberDataflow vnaDataflow) {
        PointerUsageRequiringNonNullValue pu;
        try {
            UsagesRequiringNonNullValues usages = classContext.getUsagesRequiringNonNullValues(method);
            pu = usages.get(loc, refValue, vnaDataflow);
            if (pu == null) {
                return "SOURCE_LINE_DEREF";
            }
            return pu.getDescription();
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("Error getting UsagesRequiringNonNullValues for " + method, e);
            return "SOURCE_LINE_DEREF";
        } catch (CFGBuilderException e) {
            AnalysisContext.logError("Error getting UsagesRequiringNonNullValues for " + method, e);
            return "SOURCE_LINE_DEREF";
        }

    }
}

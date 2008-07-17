package binio.expr;

import binio.CompoundData;
import binio.SequenceType;
import binio.SimpleType;
import static binio.expr.ExpressionBuilder.*;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.IOException;

public class ExpressionBuilderTest extends TestCase {
    public void testSequence() throws IOException {
        SequenceExpr sequence = SEQ(UINT, 42);
        assertEquals(true, sequence.isConstant());
        Object result = sequence.evaluate(null);
        assertTrue(result instanceof SequenceType);
        SequenceType sequenceType = (SequenceType) result;
        Assert.assertEquals(SimpleType.UINT, sequenceType.getElementType());
        assertEquals(42, sequenceType.getElementCount());
    }

    public void testChoice() throws IOException {
        ChoiceExpr choice = IF(CONSTANT(true), INT, DOUBLE);
        assertEquals(true, choice.isConstant());
        Object result = choice.evaluate(null);
        assertTrue(result instanceof SimpleType);
        assertEquals(SimpleType.INT, result);

        choice = IF(CONSTANT(false), INT, DOUBLE);
        assertEquals(true, choice.isConstant());
        result = choice.evaluate(null);
        assertTrue(result instanceof SimpleType);
        assertEquals(SimpleType.DOUBLE, result);
    }

    public void testSelect() throws IOException {
        final VariableExpr condition = new VariableExpr();
        final SelectionExpr selection = SELECT(condition,
                                               CASE("A", FLOAT),
                                               CASE("B", DOUBLE),
                                               DEFAULT(INT));
        condition.value = "A";
        assertEquals(false, selection.isConstant());
        Object result = selection.evaluate(null);
        assertTrue(result instanceof SimpleType);
        assertEquals(SimpleType.FLOAT, result);

        condition.value = "B";
        assertEquals(false, selection.isConstant());
        result = selection.evaluate(null);
        assertTrue(result instanceof SimpleType);
        assertEquals(SimpleType.DOUBLE, result);

        condition.value = "X";
        assertEquals(false, selection.isConstant());
        result = selection.evaluate(null);
        assertTrue(result instanceof SimpleType);
        assertEquals(SimpleType.INT, result);
    }

    public void testComplexExpressionTree() {
        final CompoundExpr SNAPSHOT_INFO =
                COMP("Snapshot_Information",
                     MEMBER("Snapshot_Time", SEQ(UINT, 3)),
                     MEMBER("Snapshot_ID", UINT),
                     MEMBER("Snapshot_OBET", SEQ(UBYTE, 8)),
                     MEMBER("Position", SEQ(DOUBLE, 3)),
                     MEMBER("Velocity", SEQ(DOUBLE, 3)),
                     MEMBER("Vector_Source", UBYTE),
                     MEMBER("Q0", DOUBLE),
                     MEMBER("Q1", DOUBLE),
                     MEMBER("Q2", DOUBLE),
                     MEMBER("Q3", DOUBLE),
                     MEMBER("TEC", DOUBLE),
                     MEMBER("Geomag_F", DOUBLE),
                     MEMBER("Geomag_D", DOUBLE),
                     MEMBER("Geomag_I", DOUBLE),
                     MEMBER("Sun_RA", FLOAT),
                     MEMBER("Sun_DEC", FLOAT),
                     MEMBER("Sun_BT", FLOAT),
                     MEMBER("Accuracy", FLOAT),
                     MEMBER("Radiometric_Accuracy", SEQ(FLOAT, 2))
                );
        final CompoundExpr BT_DATA =
                COMP("Bt_Data",
                     MEMBER("Flags", USHORT),
                     MEMBER("BT_Value_Real", FLOAT),
                     MEMBER("BT_Value_Imag", FLOAT),
                     MEMBER("Radiometric_Accuracy_of_Pixel", USHORT),
                     MEMBER("Incidence_Angle", USHORT),
                     MEMBER("Azimuth_Angle", USHORT),
                     MEMBER("Faraday_Rotation_Angle", USHORT),
                     MEMBER("Geometric_Rotation_Angle", USHORT),
                     MEMBER("Snapshot_ID_of_Pixel", UINT),
                     MEMBER("Footprint_Axis1", USHORT),
                     MEMBER("Footprint_Axis2", USHORT)
                );
        final CompoundExpr GRID_POINT_DATA =
                COMP("Grid_Point_Data",
                     MEMBER("Grid_Point_ID", UINT), /*4*/
                     MEMBER("Grid_Point_Latitude", FLOAT), /*8*/
                     MEMBER("Grid_Point_Longitude", FLOAT),/*12*/
                     MEMBER("Grid_Point_Altitude", FLOAT), /*16*/
                     MEMBER("Grid_Point_Mask", UBYTE),    /*17*/
                     MEMBER("BT_Data_Counter", UBYTE),    /*18*/
                     MEMBER("Bt_Data_List",
                            SEQ(BT_DATA, IREF("BT_Data_Counter")))
                );
        final CompoundExpr MIR_SCLD1C =
                COMP("MIR_SCLD1C",
                     MEMBER("Snapshot_Counter", UINT),
                     MEMBER("Snapshot_List",
                            SEQ(SNAPSHOT_INFO, IREF("Snapshot_Counter"))),
                     MEMBER("Grid_Point_Counter", UINT),
                     MEMBER("Grid_Point_List",
                            SEQ(GRID_POINT_DATA, IREF("Grid_Point_Counter")))
                );

    }

    private static class VariableExpr extends AbstractExpression {
        Object value;

        public boolean isConstant() {
            return false;
        }

        public Object evaluate(CompoundData context) throws IOException {
            return value;
        }
    }
}
package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.utils.Matrix;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MatrixValue extends Value implements ContainerValueInterface{

    private Matrix matrix;


    public MatrixValue(int rows, int columns){this.matrix=new Matrix(rows, columns);}

    public MatrixValue(Matrix m){this.matrix=m;}

    public MatrixValue(double[][] data){this.matrix=new Matrix(data);}//useless, i think, cos of function below

    public MatrixValue(ListValue lv){
        List<Value> l = lv.getItems();
        int rows=l.size();
        int columns=l.get(0).length();
        double[][] data=new double[rows][columns];
        for(int r=0;r<rows;r++){
            Value rv=l.get(r);
            if(!(rv instanceof ListValue))
                throw new InternalExpressionException("Must have a list of lists of numbers to make a matrix");
            List<Value> col=((ListValue) rv).getItems();
            if(col.size()<columns)//todo decide whether to replace with != check
                throw new InternalExpressionException("List is too short, must have lists of equal length to make a matrix");
            for(int c=0;c<columns;c++){
                Value cv=col.get(c);
                if(!(cv instanceof NumericValue))
                    throw new InternalExpressionException("Must have a list of lists of numbers to make a matrix");
                data[r][c]=((NumericValue) cv).getDouble();
            }
        }
        this.matrix=new Matrix(data);
    }

    public Matrix getMatrix(){
        return this.matrix;
    }

    //Matrix-specific funcs
    public int rows(){return this.matrix.rows();}
    public int columns(){return this.matrix.columns();}

    public List<Value> rowList(){
        List<Value> ret = new ArrayList<>();
        for(int r=0;r<matrix.rows();r++)
            ret.add(row(r));
        return ret;
    }

    public List<Value> columnList(){
        List<Value> ret = new ArrayList<>();
        for(int c=0;r<matrix.columns();c++)
            ret.add(column(c));
        return ret;
    }

    public Value row(int r){
        double[] row= matrix.row(r);
        if(row == null)
            return Value.NULL;

        return ListValue.wrap(Arrays.stream(row).mapToObj(NumericValue::new).collect(Collectors.toList()));
    }

    public Value column(int c){
        double[] col= matrix.column(c);
        if(col == null)
            return Value.NULL;

        return ListValue.wrap(Arrays.stream(col).mapToObj(NumericValue::new).collect(Collectors.toList()));
    }

    public static MatrixValue random(int rows, int columns){
        return new MatrixValue(Matrix.random(rows,columns));
    }

    public static MatrixValue identity(int rows){
        return new MatrixValue(Matrix.identity(rows));
    }

    @Override
    public String getString(){
        StringBuilder string= new StringBuilder();
        for (int i = 0; i < matrix.rows(); i++) {
            string.append("[");
            for (int j = 0; j < matrix.columns(); j++)
                string.append(String.format("%s%s", matrix.get(i,j),j==matrix.columns()-1?"":", "));
            string.append("]\n");//not adding commas cos of normal matrix display format, wouldn't make sense
        }
        return string.toString();
    }

    public String getPrettyString() {
        if(matrix.rows()<10&matrix.columns()<10)
            return getString();
        return String.format("[%s, %s, %s... %s, %s, %s]",
                matrix.get(0,0),
                matrix.get(0,1),
                matrix.get(0,2),
                matrix.get(matrix.rows()-1,matrix.columns()-3),
                matrix.get(matrix.rows()-1,matrix.columns()-2),
                matrix.get(matrix.rows()-1,matrix.columns()-1)
        );
    }
    public String getTypeString(){
        return "matrix";
    }

    @Override
    public boolean getBoolean(){
        return !this.matrix.eq(new Matrix(rows(),columns()));//checking if its equal to an empty matrix
    }

    @Override
    public Tag toTag(boolean force) {
        return null;
    }

    @Override
    public Value clone() {return new MatrixValue(matrix);}

    @Override
    public Value add(Value v){
        if(v instanceof MatrixValue)
            return new MatrixValue(matrix.add(((MatrixValue) v).matrix));
        return v.add(this);
    }

    public Value subtract(Value v){
        if(v instanceof MatrixValue)
            return new MatrixValue(matrix.subtract(((MatrixValue) v).matrix));
        return v.subtract(this);
    }

    public Value multiply(Value v){//todo division
        if(v instanceof MatrixValue)
            return new MatrixValue(matrix.multiply(((MatrixValue) v).matrix));

        if(v instanceof ListValue){//special case for lists which can be vectors or matrices

            if(((ListValue)v).items.stream().allMatch(i->i instanceof NumericValue)){//if its a vector as a list
                List<Value> lv = ((ListValue) v).items;
                List<Value> mlv = new ArrayList<>();

                if (lv.size() == this.rows()) {//C=list*this
                    mlv.add(ListValue.wrap(lv));
                }

                else if (lv.size() == this.columns()) {//C=this*list
                    for (Value val : lv)
                        mlv.add(ListValue.of(val));
                }

                return this.multiply(new MatrixValue(ListValue.wrap(mlv)));
            }

            try{//seeing if its a list of lists of numbers
                return this.multiply(new MatrixValue((ListValue)v));
            } catch (InternalExpressionException ignored){}//just going to normal string multiplying
        }

        if(v instanceof NumericValue)
            return new MatrixValue(matrix.multiply(((NumericValue) v).getDouble()));

        return new StringValue(getPrettyString()+"."+v.getPrettyString());
    }

    public boolean equals(final Object o){
        if(o instanceof MatrixValue)
            return matrix.eq(((MatrixValue)o).getMatrix());
        return o.equals(this);
    }

    public int length(){
        return matrix.rows()*matrix.columns();
    }

    @Override
    public boolean put(Value coords, Value value) {
        if(!(coords instanceof ListValue && coords.length()==2))
            throw new InternalExpressionException("Need a list of row and column to access items in a matrix");

        Value rowVal = ((ListValue) coords).items.get(0);
        Value columnVal = ((ListValue) coords).items.get(1);
        if(!(rowVal instanceof NumericValue && columnVal instanceof NumericValue))
            throw new InternalExpressionException("Need a list of row and column to access items in a matrix");

        int row=((NumericValue) rowVal).getInt();
        int col=((NumericValue) columnVal).getInt();
        if(!(value instanceof NumericValue))
            throw new InternalExpressionException("Cannot assign non-number value in a matrix");

        return matrix.put(row,col,((NumericValue) value).getDouble());
    }


    public Value get(Value where) {
        if(!(where instanceof ListValue && where.length()==2)) {
            throw new InternalExpressionException("Need a list of row and column to access items in a matrix");
        }
        Value rowVal = ((ListValue) where).items.get(0);
        Value columnVal = ((ListValue) where).items.get(1);

        //extra logic to get rows/columns

        //if both arent numbers, throw error
        if(!(rowVal instanceof NumericValue || columnVal instanceof NumericValue))
            throw new InternalExpressionException("Need at least 1 number to access items in a matrix");

        //if row is specified, not column, get that row
        if(rowVal instanceof NumericValue && !(columnVal instanceof NumericValue))
            return row(((NumericValue) rowVal).getInt());

        //if column is specified, not row, get that column (don't need to check for column here tho cos thats already asserted)
        if(!(rowVal instanceof NumericValue))
            return column(((NumericValue) columnVal).getInt());

        Double ret = matrix.get(((NumericValue) rowVal).getInt(),((NumericValue) columnVal).getInt());

        if(ret == null)
            return Value.NULL;

        return new NumericValue(ret);
    }

    public boolean has(Value where) {
        if(!(where instanceof ListValue && where.length()==2))
            throw new InternalExpressionException("Need a list of row and column to access items in a matrix");

        Value rowVal = ((ListValue) where).items.get(0);
        Value columnVal = ((ListValue) where).items.get(1);
        if(!(rowVal instanceof NumericValue && columnVal instanceof NumericValue))
            throw new InternalExpressionException("Need a list of row and column to access items in a matrix");

        return matrix.has(((NumericValue) rowVal).getInt(),((NumericValue) columnVal).getInt());
    }

    public boolean delete(Value where) {//setting to 0 cos cant delete from matrix
        if(!(where instanceof ListValue && where.length()==2))
            throw new InternalExpressionException("Need a list of row and column to access items in a matrix");

        Value rowVal = ((ListValue) where).items.get(0);
        Value columnVal = ((ListValue) where).items.get(1);
        if(!(rowVal instanceof NumericValue && columnVal instanceof NumericValue))
            throw new InternalExpressionException("Need a list of row and column to access items in a matrix");

        int row=((NumericValue) rowVal).getInt();
        int col=((NumericValue) columnVal).getInt();

        boolean ret = matrix.has(row,col)&&matrix.get(row,col)!=0;

        if(ret)
            matrix.put(row,col,0D);

        return ret;
    }

    public Value in(Value what){
        for(int r=0;r<matrix.rows();r++)
            for(int c=0;c<matrix.columns();c++)
                if(matrix.get(r,c)==what.readDoubleNumber()) return new NumericValue(what.readDoubleNumber());
        return Value.NULL;
    }
}

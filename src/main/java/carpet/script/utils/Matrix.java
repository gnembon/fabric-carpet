package carpet.script.utils;

import carpet.script.exception.InternalExpressionException;

public class Matrix {
    private final int M;             // number of rows
    private final int N;             // number of columns
    private double[][] data;         // M-by-N array

    // M-by-N matrix of 0's
    public Matrix(int M, int N) {
        this.M = M;
        this.N = N;
        data = new double[M][N];
    }

    // create matrix based on 2d array
    public Matrix(double[][] data) {
        M = data.length;
        N = data[0].length;
        this.data = new double[M][N];
        for (int i = 0; i < M; i++)
            System.arraycopy(data[i], 0, this.data[i], 0, N);
    }

    //access values
    public double[][] data(){
        return this.data;
    }

    public int rows(){
        return this.M;
    }

    public int columns(){
        return this.N;
    }

    //functions for ContainerValueInterface

    public double get(int r, int c) {
        return data[r][c];
    }

    public double[] row(int r){
        if(r>=M)
            throw new InternalExpressionException("Index "+r+" out of bounds for row length "+M);
        return data[r];
    }

    public double[] column(int c){
        if(c>=N)
            throw new InternalExpressionException("Index "+c+" out of bounds for column length "+N);
        double[] ret=new double[N];
        for(int r=0;r<M;r++)
            ret[r]=data[r][c];
        return ret;
    }

    public boolean put(int r, int c, double value){
        boolean ret=r<M&r>=0&c<N&c>=0&data[r][c]!=value;
        if(ret)
            data[r][c]=value;
        return ret;
    }

    public boolean has(int r, int c){
        return r<M&r>=0&c<N&c>=0;
    }

    // random M-by-N matrix with values between 0 and 1
    public static Matrix random(int M, int N) {
        Matrix A = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                A.data[i][j] = Math.random();
        return A;
    }

    // N-by-N identity matrix
    public static Matrix identity(int N) {
        Matrix I = new Matrix(N, N);
        for (int i = 0; i < N; i++)
            I.data[i][i] = 1;
        return I;
    }

    // C = A + B
    public Matrix add(Matrix B) {
        Matrix A = this;
        if (B.M != A.M || B.N != A.N) throw new InternalExpressionException("Illegal matrix dimensions.");
        Matrix C = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                C.data[i][j] = A.data[i][j] + B.data[i][j];
        return C;
    }


    // C = A - B
    public Matrix subtract(Matrix B) {
        Matrix A = this;
        if (B.M != A.M || B.N != A.N) throw new InternalExpressionException("Illegal matrix dimensions.");
        Matrix C = new Matrix(M, N);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                C.data[i][j] = A.data[i][j] - B.data[i][j];
        return C;
    }

    // A == B exactly
    public boolean eq(Matrix B) {
        Matrix A = this;
        if (B.M != A.M || B.N != A.N) return false;//had error here, but realised it just means false
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                if (A.data[i][j] != B.data[i][j]) return false;
        return true;
    }

    // C = A * B (for both scalar and other matrix ofc)
    public Matrix multiply(Matrix B) {
        Matrix A = this;
        if (A.N != B.M) throw new InternalExpressionException("Illegal matrix dimensions.");
        Matrix C = new Matrix(A.M, B.N);
        for (int i = 0; i < C.M; i++)
            for (int j = 0; j < C.N; j++)
                for (int k = 0; k < A.N; k++)
                    C.data[i][j] += (A.data[i][k] * B.data[k][j]);
        return C;
    }

    public Matrix multiply(double B){
        Matrix A = this;
        Matrix C = new Matrix(A.M,A.N);
        for(int r=0; r<C.M;r++)
            for(int c=0;c<C.N;c++)
                C.data[r][c]=A.data[r][c]*B;
        return C;
    }

    //transpose

    public Matrix transpose() {//todo for division
        Matrix A = new Matrix(N, M);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                A.data[j][i] = this.data[i][j];
        return A;
    }

    //division stuff

    public double determinant(){//this code is messy cos internet copypasta
        if(M!=N)
            throw new InternalExpressionException("Cannot take determinant of non-square matrix");

        if (M == 1)
            return (data[0][0]);

        else if (M == 2)
            return (data[0][0]*data[1][1] - data[1][0]*data[0][1]);

        else {
            Matrix minor = new Matrix(M-1,N-1);

            int row_minor, column_minor = 0;
            int sum = 0;

            // exclude first row and current column
            for(int firstrow_columnindex = 0; firstrow_columnindex < M;
                firstrow_columnindex++) {

                row_minor = 0;

                for(int row = 1; row < M; row++) {

                    column_minor = 0;

                    for(int column = 0; column < N; column++) {
                        if (column == firstrow_columnindex)
                            continue;
                        else
                            minor.put(row_minor,column_minor,data[row][column]);

                        column_minor++;
                    }

                    row_minor++;
                }

                if (firstrow_columnindex % 2 == 0)
                    sum += data[0][firstrow_columnindex] * minor.determinant();
                else
                    sum -= data[0][firstrow_columnindex] * minor.determinant();

            }

            return sum;

        }
    }
}

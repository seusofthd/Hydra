package com.symlab.hydraapp;

import java.io.Serializable;

public class Sorting  implements Serializable {
	
	private static final long serialVersionUID = 434193638395877253L;
	
	public int[] arr;
	
	public void qSort() {
		for (int k = 0; k < 100; k++) {
			arr = new int[20000];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = (int) (Math.random()*100000);
			}
			quickSort(0,arr.length-1);
		}
	}
	
	public boolean quickSort(int low, int high) {
 
		if (arr == null || arr.length == 0)
			return true;
 
		if (low >= high)
			return true;
 
		//pick the pivot
		int middle = low + (high - low) / 2;
		int pivot = arr[middle];
 
		//make left < pivot and right > pivot
		int i = low, j = high;
		while (i <= j) {
			while (arr[i] < pivot) {
				i++;
			}
 
			while (arr[j] > pivot) {
				j--;
			}
 
			if (i <= j) {
				int temp = arr[i];
				arr[i] = arr[j];
				arr[j] = temp;
				i++;
				j--;
			}
		}
 
		//recursively sort two sub parts
		if (low < j)
			quickSort(low, j);
 
		if (high > i)
			quickSort(i, high);
		return true;
	}
 
	public static void printArray(int[] x) {
		for (int a : x)
			System.out.print(a + " ");
		System.out.println();
	}
	
	public void setArray(int[] a) {
		arr = a;
	}
	
	public int[] getArray() {
		return arr;
	}


}

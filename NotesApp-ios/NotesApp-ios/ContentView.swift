//
//  ContentView.swift
//  NotesApp-ios
//
//  Created by Eli Pacheco Hoyos on 3/4/26.
//

import SwiftUI
import notes_module

struct ContentView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Hello, world!")
        }
        .padding()
        .onAppear {
            print("Result: \(CustomFibiKt.generateNumberList())")
        }
    }
}

#Preview {
    ContentView()
}

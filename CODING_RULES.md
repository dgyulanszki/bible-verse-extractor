# Coding Rules for GPT 5.4

- **Use Modern Java Features**
    - Utilize Java features up to version 25 where appropriate, ensuring code remains readable and maintainable.

- **Code Formatting & Imports**
    - Always auto-format modified classes.
    - Reorganize imports to remove unused and duplicate entries.

- **Variable Declaration**
    - Avoid using the `var` keyword; declare variable types explicitly.

- **Code Quality**
    - Write clean, readable, and maintainable code.
    - Follow best practices for naming, structure, and documentation.

- **Testing**
    - Write tests to achieve 100% line coverage.
    - Include tests for edge cases and potential failure scenarios.

- **File Endings**
    - Do not add multiple empty lines at the end of files; ensure only a single empty line (if any) is present.

- **Method Ordering**
    - Public methods should be listed before private methods in each class.
    - When adding new public methods, place them at the end of the public methods section.
    - When adding new private methods, place them at the end of the class, after all public methods.
export default function Footer() {
  return (
    <footer className="bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-800 py-8 px-4 sm:px-6 lg:px-8 transition-colors">
      <div className="max-w-7xl mx-auto">
        <div className="flex flex-col sm:flex-row justify-between items-center">
          <div className="text-gray-600 dark:text-gray-400 text-sm mb-4 sm:mb-0">
            © 2023 GigWave Inc.
          </div>
          <div className="flex space-x-6">
            <a href="/terms" className="text-gray-600 dark:text-gray-400 hover:text-teal-600 dark:hover:text-teal-400 text-sm transition-colors">
              Terms
            </a>
            <a href="/privacy" className="text-gray-600 dark:text-gray-400 hover:text-teal-600 dark:hover:text-teal-400 text-sm transition-colors">
              Privacy
            </a>
            <a href="/support" className="text-gray-600 dark:text-gray-400 hover:text-teal-600 dark:hover:text-teal-400 text-sm transition-colors">
              Support
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}

